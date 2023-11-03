#version 460
#extension GL_EXT_ray_tracing : enable
#extension GL_EXT_nonuniform_qualifier : enable

layout(set = 0, binding = 3) buffer DebugBuffer { float arr[]; } debugBuffer;

layout(location = 0) rayPayloadInEXT vec3 hitValue;
hitAttributeEXT vec2 attribs;

void main() {
  const vec3 barycentricCoords = vec3(1.0f - attribs.x - attribs.y, attribs.x, attribs.y);
  hitValue = barycentricCoords;

  debugBuffer.arr[10] = 1.0;

  hitValue = vec3(1.0,1.0,1.0);
}