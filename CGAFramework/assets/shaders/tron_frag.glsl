#version 330 core

uniform sampler2D emit;

//input from vertex shader
in struct VertexData
{
    vec2 texcoords;
} vertexData;

//fragment shader output
out vec4 color;

void main(){

    //color = vec4(0, (0.5f + abs(vertexData.position.z)), 0, 1.0f);
    color = vec4(texture(emit, vertexData.texcoords).rgb,1.0f);
    //color = vec4(1.0f,0.0f,1.0f,1.0f);
    //color = vec4(normalize(abs(vertexData.normal)), 1.0f);
    //color = vec2(texcoords);

}