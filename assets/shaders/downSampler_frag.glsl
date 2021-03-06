#version 330 core
// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0

in vec2 v_uv0;

uniform sampler2D tex;
uniform float size;

const vec2 s1 = vec2(-1, 1);
const vec2 s2 = vec2( 1, 1);
const vec2 s3 = vec2( 1,-1);
const vec2 s4 = vec2(-1,-1);

layout(location = 0) out vec4 outColor;

void main() {
	vec2 texCoordSample = vec2(0.0);

	texCoordSample = v_uv0.xy + s1 / size;
	vec4 color = texture(tex, texCoordSample);

	texCoordSample = v_uv0.xy + s2 / size;
	color += texture(tex, texCoordSample);

	texCoordSample = v_uv0.xy + s3 / size;
	color += texture(tex, texCoordSample);

	texCoordSample = v_uv0.xy + s4 / size;
	color += texture(tex, texCoordSample);

	outColor = color * 0.25;
}
