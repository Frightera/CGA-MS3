#version 330 core

layout(location = 0) in vec3 position;
layout(location = 2) in vec3 normal;
layout(location = 1) in vec2 texcoords;

//uniforms
//translation object to world
uniform mat4 model_matrix;
uniform mat4 view;
uniform mat4 projection;
uniform vec2 tcMultiplier;


out struct VertexData
{
    vec2 texcoords;
} vertexData;

//
void main(){
    vec4 pos = projection * view * model_matrix * vec4(position, 1.0f);

    gl_Position = pos;
    vertexData.texcoords = tcMultiplier * texcoords;
}