#version 460
#extension GL_EXT_ray_tracing : enable
#extension GL_EXT_nonuniform_qualifier : enable

layout(set = 0, binding = 3) buffer DebugBuffer { float arr[]; } debugBuffer;

layout(location = 0) rayPayloadInEXT vec3 hitValue;
hitAttributeEXT vec3 hitPosition;

void main() {
    hitValue = hitPosition;
}