package com.procut.render

/**
 * Clean-room GLSL shaders for OpenGL ES 3.0 multi-pass video compositing,
 * color grading, chroma keying, geometric masking, and clip transitions.
 */
object ShaderSources {

    // --- Vertex Shaders ---

    val VERTEX_TRANSFORM = """
        #version 300 es
        layout(location = 0) in vec4 a_Position;
        layout(location = 1) in vec2 a_TexCoord;

        uniform mat4 u_MVPMatrix;
        uniform mat4 u_STMatrix;

        out vec2 v_TexCoord;

        void main() {
            gl_Position = u_MVPMatrix * a_Position;
            v_TexCoord = (u_STMatrix * vec4(a_TexCoord, 0.0, 1.0)).xy;
        }
    """.trimIndent()

    val VERTEX_PASSTHROUGH = """
        #version 300 es
        layout(location = 0) in vec4 a_Position;
        layout(location = 1) in vec2 a_TexCoord;

        out vec2 v_TexCoord;

        void main() {
            gl_Position = a_Position;
            v_TexCoord = a_TexCoord;
        }
    """.trimIndent()

    // --- Canvas Background Shaders ---

    val FRAGMENT_CANVAS_SOLID = """
        #version 300 es
        precision mediump float;
        out vec4 FragColor;
        uniform vec4 u_BackgroundColor;

        void main() {
            FragColor = u_BackgroundColor;
        }
    """.trimIndent()

    val FRAGMENT_CANVAS_GRADIENT = """
        #version 300 es
        precision mediump float;
        in vec2 v_TexCoord;
        out vec4 FragColor;

        uniform vec4 u_StartColor;
        uniform vec4 u_EndColor;
        uniform float u_AngleRad; // Gradient angle in radians

        void main() {
            // Project UV onto gradient direction vector
            vec2 dir = vec2(cos(u_AngleRad), sin(u_AngleRad));
            vec2 centered = v_TexCoord - vec2(0.5);
            float t = clamp(dot(centered, dir) + 0.5, 0.0, 1.0);
            FragColor = mix(u_StartColor, u_EndColor, t);
        }
    """.trimIndent()

    val FRAGMENT_CANVAS_GAUSSIAN_BLUR = """
        #version 300 es
        precision mediump float;
        in vec2 v_TexCoord;
        out vec4 FragColor;

        uniform sampler2D u_Texture;
        uniform vec2 u_Direction; // (1.0/w, 0.0) for horizontal, (0.0, 1.0/h) for vertical
        uniform float u_BlurRadius;
        uniform float u_Darken; // 0.0 to 1.0 darkening factor

        void main() {
            vec4 sum = vec4(0.0);
            // 9-tap Gaussian kernel weights
            float weights[5] = float[](0.227027, 0.1945946, 0.1216216, 0.054054, 0.016216);

            sum += texture(u_Texture, v_TexCoord) * weights[0];
            for (int i = 1; i < 5; i++) {
                vec2 offset = u_Direction * float(i) * u_BlurRadius;
                sum += texture(u_Texture, v_TexCoord + offset) * weights[i];
                sum += texture(u_Texture, v_TexCoord - offset) * weights[i];
            }
            FragColor = vec4(sum.rgb * (1.0 - u_Darken), sum.a);
        }
    """.trimIndent()

    // --- Primary 12-Parameter Color Grading Fragment Shader ---

    val FRAGMENT_COLOR_GRADING = """
        #version 300 es
        precision highp float;
        in vec2 v_TexCoord;
        out vec4 FragColor;

        uniform sampler2D u_Texture;

        // 12 Color Adjustment Parameters
        uniform float u_Brightness;  // [-1.0, 1.0]
        uniform float u_Contrast;    // [-1.0, 1.0]
        uniform float u_Saturation;  // [-1.0, 1.0]
        uniform float u_Exposure;    // [-1.0, 1.0]
        uniform float u_Sharpen;     // [0.0, 1.0]
        uniform float u_Highlights;  // [-1.0, 1.0]
        uniform float u_Shadows;     // [-1.0, 1.0]
        uniform float u_Temperature; // [-1.0, 1.0]
        uniform float u_Tint;        // [-1.0, 1.0]
        uniform float u_Vignette;    // [0.0, 1.0]
        uniform float u_Fade;        // [0.0, 1.0]
        uniform float u_Grain;       // [0.0, 1.0]

        uniform vec2 u_TexelSize;    // (1.0/w, 1.0/h)
        uniform float u_Time;        // Time in seconds for animated grain

        const vec3 W = vec3(0.2126, 0.7152, 0.0722); // Rec.709 Luma coefficients

        // Pseudo-random noise hash for film grain
        float rand(vec2 co) {
            return fract(sin(dot(co.xy, vec2(12.9898, 78.233))) * 43758.5453);
        }

        void main() {
            vec4 src = texture(u_Texture, v_TexCoord);
            vec3 color = src.rgb;

            // 1. Sharpen (Laplacian unsharp masking)
            if (u_Sharpen > 0.0) {
                vec3 n = texture(u_Texture, v_TexCoord + vec2(0.0, u_TexelSize.y)).rgb;
                vec3 s = texture(u_Texture, v_TexCoord - vec2(0.0, u_TexelSize.y)).rgb;
                vec3 e = texture(u_Texture, v_TexCoord + vec2(u_TexelSize.x, 0.0)).rgb;
                vec3 w = texture(u_Texture, v_TexCoord - vec2(u_TexelSize.x, 0.0)).rgb;
                vec3 edge = 4.0 * color - (n + s + e + w);
                color += edge * (u_Sharpen * 0.7);
            }

            // 2. Exposure: C' = C * 2^(exposure * 2.0)
            color *= exp2(u_Exposure * 2.0);

            // 3. Brightness: C' = C + brightness * 0.3
            color += u_Brightness * 0.3;

            // 4. Contrast: C' = (C - 0.5) * (1.0 + contrast) + 0.5
            float contrastMult = (u_Contrast > 0.0) ? (1.0 + u_Contrast) : (1.0 + u_Contrast * 0.5);
            color = (color - 0.5) * contrastMult + 0.5;

            // 5. White Balance: Temperature (Amber/Blue) & Tint (Magenta/Green)
            color.r += u_Temperature * 0.15;
            color.b -= u_Temperature * 0.15;
            color.g += u_Tint * 0.15;

            // 6. Highlights & Shadows (Soft knee Hermite curve)
            float luma = dot(color, W);
            float highlightWeight = smoothstep(0.5, 1.0, luma);
            float shadowWeight = 1.0 - smoothstep(0.0, 0.5, luma);
            color += u_Highlights * 0.2 * highlightWeight;
            color += u_Shadows * 0.2 * shadowWeight;

            // 7. Saturation: mix(Luma, Color, 1.0 + saturation)
            float satMult = (u_Saturation > 0.0) ? (1.0 + u_Saturation) : (1.0 + u_Saturation);
            float currentLuma = dot(color, W);
            color = mix(vec3(currentLuma), color, satMult);

            // 8. Fade (Lift blacks and soften shadows)
            if (u_Fade > 0.0) {
                color = color * (1.0 - u_Fade * 0.4) + vec3(u_Fade * 0.2);
            }

            // 9. Film Grain
            if (u_Grain > 0.0) {
                float noise = (rand(v_TexCoord + vec2(fract(u_Time), fract(u_Time * 1.618))) - 0.5) * 2.0;
                color += noise * (u_Grain * 0.15);
            }

            // 10. Vignette (Circular cosine falloff)
            if (u_Vignette > 0.0) {
                vec2 uv = (v_TexCoord - 0.5) * 2.0;
                float dist = length(uv);
                float vig = smoothstep(0.8, 1.5 - (u_Vignette * 0.5), dist);
                color *= (1.0 - vig * u_Vignette * 0.8);
            }

            FragColor = vec4(clamp(color, 0.0, 1.0), src.a);
        }
    """.trimIndent()

    // --- Chroma Keying Fragment Shader ---

    val FRAGMENT_CHROMA_KEY = """
        #version 300 es
        precision highp float;
        in vec2 v_TexCoord;
        out vec4 FragColor;

        uniform sampler2D u_Texture;
        uniform vec3 u_KeyColorRGB;    // Target key color [0..1]
        uniform float u_Threshold;     // Similarity tolerance [0..1]
        uniform float u_ShadowFeather; // Edge feathering softness [0..1]
        uniform float u_SpillReduction;// Spill suppression strength [0..1]

        // Converts RGB to YCbCr chrominance components
        vec2 rgbToCbCr(vec3 rgb) {
            float cb = -0.168736 * rgb.r - 0.331264 * rgb.g + 0.500000 * rgb.b;
            float cr =  0.500000 * rgb.r - 0.418688 * rgb.g - 0.081312 * rgb.b;
            return vec2(cb, cr);
        }

        void main() {
            vec4 src = texture(u_Texture, v_TexCoord);
            vec3 color = src.rgb;

            vec2 pixelCbCr = rgbToCbCr(color);
            vec2 keyCbCr = rgbToCbCr(u_KeyColorRGB);

            // Euclidean distance in chrominance space (Cb, Cr)
            float dist = length(pixelCbCr - keyCbCr);

            // Alpha feathering calculation
            float alpha = clamp((dist - u_Threshold) / (u_ShadowFeather + 0.0001), 0.0, 1.0);

            // Spill suppression (Green / Blue edge color decontamination)
            if (u_SpillReduction > 0.0 && alpha < 1.0) {
                // If keying green: suppress green channel towards max(red, blue)
                if (u_KeyColorRGB.g > u_KeyColorRGB.r && u_KeyColorRGB.g > u_KeyColorRGB.b) {
                    float maxRB = max(color.r, color.b);
                    color.g = mix(color.g, min(color.g, maxRB * 0.95), u_SpillReduction);
                }
                // If keying blue: suppress blue channel towards max(red, green)
                else if (u_KeyColorRGB.b > u_KeyColorRGB.r && u_KeyColorRGB.b > u_KeyColorRGB.g) {
                    float maxRG = max(color.r, color.g);
                    color.b = mix(color.b, min(color.b, maxRG * 0.95), u_SpillReduction);
                }
            }

            FragColor = vec4(color, src.a * alpha);
        }
    """.trimIndent()

    // --- Geometric Masking Fragment Shader ---

    val FRAGMENT_GEOMETRIC_MASK = """
        #version 300 es
        precision highp float;
        in vec2 v_TexCoord;
        out vec4 FragColor;

        uniform sampler2D u_Texture;

        // Mask parameters
        uniform int   u_MaskType;     // 0: None, 1: Linear, 2: Mirror, 3: Circle, 4: Rectangle, 5: Star
        uniform vec2  u_Center;       // [0, 1] normalized center
        uniform vec2  u_Size;         // (width, height)
        uniform float u_RotationRad;  // Rotation in radians
        uniform float u_Feather;      // [0, 1] edge softness
        uniform float u_RoundCorner;  // [0, 1] for rectangle
        uniform bool  u_Invert;       // Inversion toggle
        uniform float u_AspectRatio;  // Canvas aspect ratio

        // 2D Signed Distance Function for Rounded Box
        float sdRoundedBox(vec2 p, vec2 b, float r) {
            vec2 q = abs(p) - b + r;
            return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
        }

        // 2D Signed Distance Function for 5-pointed Star
        float sdStar5(in vec2 p, in float r, in float rf) {
            const vec2 k1 = vec2(0.809016994375, -0.587785252292);
            const vec2 k2 = vec2(-k1.x, k1.y);
            p.x = abs(p.x);
            p -= 2.0 * max(dot(k1, p), 0.0) * k1;
            p -= 2.0 * max(dot(k2, p), 0.0) * k2;
            p.x = abs(p.x);
            p.y -= r;
            vec2 ba = rf * vec2(-k1.y, k1.x) - vec2(0.0, 1.0);
            float h = clamp(dot(p, ba) / dot(ba, ba), 0.0, r);
            return length(p - ba * h) * sign(p.y * ba.x - p.x * ba.y);
        }

        void main() {
            vec4 src = texture(u_Texture, v_TexCoord);
            if (u_MaskType == 0) {
                FragColor = src;
                return;
            }

            // Transform UV into local centered, rotated, aspect-corrected coordinate space
            vec2 delta = v_TexCoord - u_Center;
            delta.y *= u_AspectRatio;

            float cosR = cos(-u_RotationRad);
            float sinR = sin(-u_RotationRad);
            vec2 p = vec2(
                cosR * delta.x - sinR * delta.y,
                sinR * delta.x + cosR * delta.y
            );

            float alpha = 1.0;
            float f = max(u_Feather * 0.25, 0.0001);

            // 1. Linear Mask
            if (u_MaskType == 1) {
                float d = p.y;
                alpha = smoothstep(-f, f, d);
            }
            // 2. Mirror Mask
            else if (u_MaskType == 2) {
                float halfH = u_Size.y * 0.5;
                float d = abs(p.y) - halfH;
                alpha = smoothstep(f, -f, d);
            }
            // 3. Circle / Ellipse Mask
            else if (u_MaskType == 3) {
                vec2 radii = max(u_Size * 0.5, vec2(0.001));
                float d = length(p / radii) - 1.0;
                alpha = smoothstep(0.0, -f, d);
            }
            // 4. Rounded Rectangle Mask
            else if (u_MaskType == 4) {
                vec2 halfSize = max(u_Size * 0.5, vec2(0.001));
                float maxR = min(halfSize.x, halfSize.y);
                float r = u_RoundCorner * maxR;
                float d = sdRoundedBox(p, halfSize, r);
                alpha = smoothstep(0.0, -f, d);
            }
            // 5. Star Mask
            else if (u_MaskType == 5) {
                float starRadius = max(u_Size.x * 0.5, 0.001);
                float d = sdStar5(p, starRadius, 0.45);
                alpha = smoothstep(0.0, -f, d);
            }

            if (u_Invert) {
                alpha = 1.0 - alpha;
            }

            FragColor = vec4(src.rgb, src.a * alpha);
        }
    """.trimIndent()

    // --- Clip Transitions Fragment Shaders ---

    val FRAGMENT_TRANSITION = """
        #version 300 es
        precision highp float;
        in vec2 v_TexCoord;
        out vec4 FragColor;

        uniform sampler2D u_TextureA; // Outgoing clip
        uniform sampler2D u_TextureB; // Incoming clip
        uniform float u_Progress;     // Transition progress [0.0, 1.0]
        uniform int   u_Type;         // 0: Dissolve, 1: Zoom, 2: SlideLeft, 3: SlideRight, 4: Glitch, 5: Blur, 6: Wipe

        const float PI = 3.14159265359;

        // 1. Cross Dissolve
        vec4 transitionDissolve(vec2 uv, float p) {
            vec4 a = texture(u_TextureA, uv);
            vec4 b = texture(u_TextureB, uv);
            return mix(a, b, p);
        }

        // 2. Zoom & Spin
        vec4 transitionZoom(vec2 uv, float p) {
            vec2 center = vec2(0.5);
            float scale = 1.0 + sin(p * PI) * 0.6;
            vec2 scaledUV = (uv - center) / scale + center;
            bool outOfBounds = (scaledUV.x < 0.0 || scaledUV.x > 1.0 || scaledUV.y < 0.0 || scaledUV.y > 1.0);
            if (outOfBounds) return vec4(0.0, 0.0, 0.0, 1.0);

            vec4 a = texture(u_TextureA, scaledUV);
            vec4 b = texture(u_TextureB, scaledUV);
            return mix(a, b, smoothstep(0.4, 0.6, p));
        }

        // 3. Directional Slide
        vec4 transitionSlide(vec2 uv, float p, vec2 direction) {
            vec2 offsetA = direction * p;
            vec2 offsetB = direction * (p - 1.0);
            vec2 uvA = uv + offsetA;
            vec2 uvB = uv + offsetB;

            if (uvA.x >= 0.0 && uvA.x <= 1.0 && uvA.y >= 0.0 && uvA.y <= 1.0) {
                return texture(u_TextureA, uvA);
            } else {
                return texture(u_TextureB, uvB);
            }
        }

        // 4. Glitch (Chromatic aberration & sine distortion)
        vec4 transitionGlitch(vec2 uv, float p) {
            float intensity = sin(p * PI);
            float rOffset = sin(uv.y * 40.0 + p * 20.0) * 0.04 * intensity;
            float bOffset = -rOffset;

            vec4 colorA = vec4(
                texture(u_TextureA, uv + vec2(rOffset, 0.0)).r,
                texture(u_TextureA, uv).g,
                texture(u_TextureA, uv + vec2(bOffset, 0.0)).b,
                1.0
            );
            vec4 colorB = vec4(
                texture(u_TextureB, uv + vec2(rOffset, 0.0)).r,
                texture(u_TextureB, uv).g,
                texture(u_TextureB, uv + vec2(bOffset, 0.0)).b,
                1.0
            );
            return mix(colorA, colorB, smoothstep(0.35, 0.65, p));
        }

        // 5. Directional Blur Transition
        vec4 transitionBlur(vec2 uv, float p) {
            float blurAmount = sin(p * PI) * 0.05;
            vec4 sumA = vec4(0.0);
            vec4 sumB = vec4(0.0);
            for (int i = -3; i <= 3; i++) {
                vec2 off = vec2(float(i) * blurAmount * 0.3, 0.0);
                sumA += texture(u_TextureA, uv + off);
                sumB += texture(u_TextureB, uv + off);
            }
            sumA /= 7.0;
            sumB /= 7.0;
            return mix(sumA, sumB, smoothstep(0.4, 0.6, p));
        }

        // 6. Horizontal Alpha Wipe
        vec4 transitionWipe(vec2 uv, float p) {
            float feather = 0.08;
            float threshold = uv.x;
            float alpha = smoothstep(p - feather, p + feather, threshold);
            vec4 a = texture(u_TextureA, uv);
            vec4 b = texture(u_TextureB, uv);
            return mix(b, a, alpha);
        }

        void main() {
            if (u_Type == 0) {
                FragColor = transitionDissolve(v_TexCoord, u_Progress);
            } else if (u_Type == 1) {
                FragColor = transitionZoom(v_TexCoord, u_Progress);
            } else if (u_Type == 2) {
                FragColor = transitionSlide(v_TexCoord, u_Progress, vec2(-1.0, 0.0)); // Left
            } else if (u_Type == 3) {
                FragColor = transitionSlide(v_TexCoord, u_Progress, vec2(1.0, 0.0));  // Right
            } else if (u_Type == 4) {
                FragColor = transitionGlitch(v_TexCoord, u_Progress);
            } else if (u_Type == 5) {
                FragColor = transitionBlur(v_TexCoord, u_Progress);
            } else if (u_Type == 6) {
                FragColor = transitionWipe(v_TexCoord, u_Progress);
            } else {
                FragColor = transitionDissolve(v_TexCoord, u_Progress);
            }
        }
    """.trimIndent()
}
