#!/usr/bin/env node

const fs = require("fs");
const path = require("path");

if (process.argv.length < 6) {
  console.error("Usage: node tools/convert_bbmodel_to_geckolib.js <input.bbmodel> <modid> <entity_name> <resources_dir>");
  process.exit(1);
}

const [, , inputPath, modId, entityName, resourcesDir] = process.argv;
const model = JSON.parse(fs.readFileSync(inputPath, "utf8"));
const elementsByUuid = new Map((model.elements || []).map((element) => [element.uuid, element]));
const usedBoneNames = new Map();
const boneNamesByUuid = new Map();

function uniqueBoneName(name) {
  const base = sanitizeName(name || "bone");
  const count = usedBoneNames.get(base) || 0;
  usedBoneNames.set(base, count + 1);
  return count === 0 ? base : `${base}_${count + 1}`;
}

function sanitizeName(name) {
  return String(name).replace(/[^A-Za-z0-9_]/g, "_").replace(/^_+/, "") || "bone";
}

function vector(value, fallback = [0, 0, 0]) {
  if (!Array.isArray(value)) {
    return fallback;
  }
  return value.map((entry) => Number(entry) || 0);
}

function faceUv(face) {
  if (!face || face.texture == null || !Array.isArray(face.uv)) {
    return null;
  }

  const [u1, v1, u2, v2] = face.uv.map((entry) => Number(entry) || 0);
  return {
    uv: [Math.min(u1, u2), Math.min(v1, v2)],
    uv_size: [Math.abs(u2 - u1), Math.abs(v2 - v1)]
  };
}

function convertCube(element) {
  if (!element || element.export === false || element.visibility === false) {
    return null;
  }

  const from = vector(element.from);
  const to = vector(element.to);
  const size = [to[0] - from[0], to[1] - from[1], to[2] - from[2]];
  const nonZeroDimensions = size.filter((entry) => Math.abs(entry) >= 0.001).length;
  if (nonZeroDimensions < 2) {
    return null;
  }

  const cube = {
    origin: from,
    size,
    uv: {}
  };

  for (const direction of ["north", "east", "south", "west", "up", "down"]) {
    const uv = faceUv(element.faces && element.faces[direction]);
    if (uv) {
      cube.uv[direction] = uv;
    }
  }

  if (Object.keys(cube.uv).length === 0) {
    return null;
  }

  if (Array.isArray(element.origin)) {
    cube.pivot = vector(element.origin);
  }
  if (Array.isArray(element.rotation)) {
    cube.rotation = vector(element.rotation);
  }
  if (typeof element.inflate === "number" && element.inflate !== 0) {
    cube.inflate = element.inflate;
  }

  return cube;
}

function convertGroup(group, parentName, bones) {
  if (!group || group.name === "hitbox" || group.export === false || group.visibility === false) {
    return;
  }

  const boneName = uniqueBoneName(group.name);
  boneNamesByUuid.set(group.uuid, boneName);
  const bone = {
    name: boneName,
    pivot: vector(group.origin)
  };
  if (parentName) {
    bone.parent = parentName;
  }
  if (Array.isArray(group.rotation)) {
    bone.rotation = vector(group.rotation);
  }

  const nestedGroups = [];
  for (const child of group.children || []) {
    if (typeof child === "string") {
      const element = elementsByUuid.get(child);
      if (!element) {
        continue;
      }
      const cube = convertCube(element);
      if (cube) {
        bone.cubes = bone.cubes || [];
        bone.cubes.push(cube);
      }
    } else if (child && typeof child === "object") {
      nestedGroups.push(child);
    }
  }

  bones.push(bone);
  for (const childGroup of nestedGroups) {
    convertGroup(childGroup, boneName, bones);
  }
}

function convertGeometry() {
  const bones = [];
  for (const group of model.outliner || []) {
    convertGroup(group, null, bones);
  }

  const resolution = model.resolution || {};
  return {
    format_version: "1.12.0",
    "minecraft:geometry": [
      {
        description: {
          identifier: `geometry.${entityName}`,
          texture_width: resolution.width || 128,
          texture_height: resolution.height || 128,
          visible_bounds_width: 4,
          visible_bounds_height: 4,
          visible_bounds_offset: [0, 1, 0]
        },
        bones
      }
    ]
  };
}

function valueOf(point) {
  const source = point || {};
  return [source.x, source.y, source.z].map((entry) => {
    const value = Number(entry);
    return Number.isFinite(value) ? value : 0;
  });
}

function addVectors(left, right) {
  return left.map((entry, index) => entry + (right[index] || 0));
}

function collectPoseOffsets() {
  const pose = (model.animations || []).find((animation) => animation.name === "pose");
  const offsets = new Map();
  if (!pose) {
    return offsets;
  }

  for (const [uuid, animator] of Object.entries(pose.animators || {})) {
    const boneName = boneNamesByUuid.get(uuid) || sanitizeName(animator.name);
    const channels = {};
    for (const keyframe of animator.keyframes || []) {
      if (Number(keyframe.time) !== 0 || !keyframe.data_points || keyframe.data_points.length === 0) {
        continue;
      }
      if (keyframe.channel === "rotation" || keyframe.channel === "position") {
        channels[keyframe.channel] = valueOf(keyframe.data_points[0]);
      }
    }
    if (Object.keys(channels).length > 0) {
      offsets.set(boneName, channels);
    }
  }

  return offsets;
}

function convertKeyframes(keyframes, channel, poseOffset) {
  const result = {};
  const sorted = keyframes
    .filter((keyframe) => keyframe.channel === channel && keyframe.data_points && keyframe.data_points.length > 0)
    .sort((left, right) => Number(left.time) - Number(right.time));

  for (const keyframe of sorted) {
    const time = Number(keyframe.time) || 0;
    const value = valueOf(keyframe.data_points[0]);
    const entry = {
      vector: poseOffset ? addVectors(value, poseOffset) : value
    };
    if (keyframe.interpolation && keyframe.interpolation !== "linear") {
      entry.easing = keyframe.interpolation;
    }
    result[String(Number(time.toFixed(4)))] = entry;
  }

  return result;
}

function addStaticPoseChannels(animation, bones, poseOffsets) {
  if (animation.name === "pose") {
    return;
  }

  const length = Number(animation.length) || 1;
  for (const [boneName, channels] of poseOffsets.entries()) {
    const boneAnimation = bones[boneName] || {};
    for (const [channel, offset] of Object.entries(channels)) {
      if (boneAnimation[channel]) {
        continue;
      }
      boneAnimation[channel] = {
        "0": {
          vector: offset
        },
        [String(Number(length.toFixed(4)))]: {
          vector: offset
        }
      };
    }
    if (Object.keys(boneAnimation).length > 0) {
      bones[boneName] = boneAnimation;
    }
  }
}

function convertAnimation(animation, poseOffsets) {
  const bones = {};
  for (const [uuid, animator] of Object.entries(animation.animators || {})) {
    const boneName = boneNamesByUuid.get(uuid) || sanitizeName(animator.name);
    const boneAnimation = {};
    for (const channel of ["rotation", "position", "scale"]) {
      const poseOffset = poseOffsets.get(boneName) && poseOffsets.get(boneName)[channel];
      const keyframes = convertKeyframes(animator.keyframes || [], channel, poseOffset);
      if (Object.keys(keyframes).length > 0) {
        boneAnimation[channel] = keyframes;
      }
    }
    if (Object.keys(boneAnimation).length > 0) {
      bones[boneName] = boneAnimation;
    }
  }
  addStaticPoseChannels(animation, bones, poseOffsets);

  return {
    loop: animation.loop === "loop",
    animation_length: Number(animation.length) || 1,
    bones
  };
}

function convertAnimations() {
  const animations = {};
  const poseOffsets = collectPoseOffsets();
  const sourceAnimations = new Map((model.animations || []).map((animation) => [animation.name, animation]));
  const mappings = [
    ["idle", "misc.idle"],
    ["walk", "move.walk"],
    ["walk_prebaked", "move.walk"],
    ["walk_force", "move.walk.force"],
    ["fly_loop", "move.fly"],
    ["fly_loop_prebaked", "move.fly"],
    ["fly_start", "move.fly.start"],
    ["fly_end", "move.fly.end"],
    ["jump", "move.jump"],
    ["attack_static", "attack.swing"],
    ["attack_moving", "attack.moving"],
    ["charge", "attack.charge"],
    ["gas", "attack.cast"],
    ["interact", "misc.interact"],
    ["eclode", "misc.spawn"],
    ["remove", "misc.remove"],
    ["death", "misc.die"]
  ];

  for (const [sourceName, targetName] of mappings) {
    const animation = sourceAnimations.get(sourceName);
    if (animation) {
      animations[targetName] = convertAnimation(animation, poseOffsets);
    }
  }

  return {
    format_version: "1.8.0",
    animations,
    geckolib_format_version: 2
  };
}

function writeJson(relativePath, data) {
  const outputPath = path.join(resourcesDir, relativePath);
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify(data, null, 2)}\n`);
}

function writeTexture() {
  const texture = (model.textures || [])[0];
  if (!texture || typeof texture.source !== "string") {
    throw new Error("No embedded texture source found in bbmodel.");
  }

  const base64 = texture.source.replace(/^data:image\/png;base64,/, "");
  const outputPath = path.join(resourcesDir, "assets", modId, "textures", "entity", `${entityName}.png`);
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, Buffer.from(base64, "base64"));
}

const geometry = convertGeometry();
const animations = convertAnimations();
writeJson(path.join("assets", modId, "geo", "entity", `${entityName}.geo.json`), geometry);
writeJson(path.join("assets", modId, "animations", "entity", `${entityName}.animation.json`), animations);
writeTexture();

console.log(`Converted ${inputPath} -> ${entityName}`);
