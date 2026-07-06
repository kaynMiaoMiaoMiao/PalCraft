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
  const from = vector(element.from);
  const to = vector(element.to);
  const size = [to[0] - from[0], to[1] - from[1], to[2] - from[2]];
  if (size.some((entry) => Math.abs(entry) < 0.001)) {
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

function convertKeyframes(keyframes, channel) {
  const result = {};
  const sorted = keyframes
    .filter((keyframe) => keyframe.channel === channel && keyframe.data_points && keyframe.data_points.length > 0)
    .sort((left, right) => Number(left.time) - Number(right.time));

  for (const keyframe of sorted) {
    const time = Number(keyframe.time) || 0;
    const entry = {
      vector: valueOf(keyframe.data_points[0])
    };
    if (keyframe.interpolation && keyframe.interpolation !== "linear") {
      entry.easing = keyframe.interpolation;
    }
    result[String(Number(time.toFixed(4)))] = entry;
  }

  return result;
}

function convertAnimation(animation, name) {
  const bones = {};
  for (const [uuid, animator] of Object.entries(animation.animators || {})) {
    const boneName = boneNamesByUuid.get(uuid) || sanitizeName(animator.name);
    const boneAnimation = {};
    for (const channel of ["rotation", "position", "scale"]) {
      const keyframes = convertKeyframes(animator.keyframes || [], channel);
      if (Object.keys(keyframes).length > 0) {
        boneAnimation[channel] = keyframes;
      }
    }
    if (Object.keys(boneAnimation).length > 0) {
      bones[boneName] = boneAnimation;
    }
  }

  return {
    loop: animation.loop === "loop",
    animation_length: Number(animation.length) || 1,
    bones
  };
}

function convertAnimations() {
  const animations = {};
  for (const animation of model.animations || []) {
    if (animation.name === "idle") {
      animations["misc.idle"] = convertAnimation(animation, "misc.idle");
    } else if (animation.name === "walk") {
      animations["move.walk"] = convertAnimation(animation, "move.walk");
    } else if (animation.name === "death") {
      animations["misc.die"] = convertAnimation(animation, "misc.die");
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
