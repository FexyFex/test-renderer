glslangValidator -V --target-env vulkan1.2 .\particle_compute.comp -o particle_compute.spv
glslangValidator -V --target-env vulkan1.2 .\standard.vert -o standard_vert.spv
glslangValidator -V --target-env vulkan1.2 .\standard.frag -o standard_frag.spv
pause