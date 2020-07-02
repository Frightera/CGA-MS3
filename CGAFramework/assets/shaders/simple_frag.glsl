#version 330 core

in vec3 col;

//fragment shader output
out vec4 color;

void main(){
    color = vec4(col, 1.0f);
}
