package com.gungame.world.objects.weapon;

import box2dLight.ConeLight;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.gungame.world.GameWorld;
import com.gungame.world.light.Lights;
import com.gungame.world.objects.meta.GameObject;
import com.gungame.world.objects.meta.GameObjectType;

public final class Laser {
    private static final ShaderProgram laserShader = createLaserShader();
    private static final TextureRegion whitePixel = createWhitePixel();

    // ───── визуальные константы ─────
    private static final int   RAYS      = 4;
    private static final Color COLOR     = Color.WHITE;
    private static final float CONE_DEG  = 0.2f;
    private static final float SOFTNESS  = 1.5f;
    private static final float MAX_LEN   = 40f;
    private static final float CORE_W    = 0.08f; // толщина сердцевины (world units)

    // ───── ссылки и состояние ─────
    private final World         world;
    private final ConeLight     light;
    private final Vector2       from     = new Vector2();
    private final Vector2       endFull  = new Vector2();
    private final Vector2       hitPoint = new Vector2();

    public Laser(GameWorld gw, Vector2 origin, float angleRad,
                 Body ignoreBody) {

        this.world = gw.getPhisicsWorld();

        from.set(origin);
        calcEnd(origin, angleRad, endFull);

        light = new ConeLight(gw.getRayHandler(), RAYS, COLOR, MAX_LEN,
                origin.x, origin.y,
                angleRad * MathUtils.radiansToDegrees,
                CONE_DEG);
        light.setSoft(true);
        light.setSoftnessLength(SOFTNESS);
        light.setStaticLight(false);
        light.setContactFilter(Lights.RAY_CONTACT_FILTER);

        castInternal(ignoreBody);
    }

    public static TextureRegion createWhitePixel() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(1, 1, 1, 1); // белый, непрозрачный
        pixmap.fill();

        Texture texture = new Texture(pixmap);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        return new TextureRegion(texture);
    }

    public static ShaderProgram createLaserShader() {
        String vertexShader = """
            attribute vec4 a_position;
            attribute vec4 a_color;
            attribute vec2 a_texCoord0;
            uniform mat4 u_projTrans;
            varying vec4 v_color;
            varying vec2 v_texCoords;
            varying vec2 v_worldPos;
            void main() {
                v_color = a_color;
                v_texCoords = a_texCoord0;
                v_worldPos = a_position.xy; // 💥 МИРОВЫЕ координаты напрямую!
                gl_Position = u_projTrans * a_position;
            }
        """;

        String fragmentShader = """
            #ifdef GL_ES
            precision mediump float;
            #endif
    
            varying vec4 v_color;
            varying vec2 v_texCoords;
            varying vec2 v_worldPos;
    
            uniform sampler2D u_texture;
            uniform vec2 u_start;
    
            void main() {
                vec4 texColor = texture2D(u_texture, v_texCoords);
    
                vec2 delta = v_worldPos - u_start;
                float dist2 = dot(delta, delta);         // расстояние²
                float fade = 1.0 - dist2 / (40.0 * 40.0); // 40 = макс. длина лазера
                fade = clamp(fade, 0.0, 1.0);
                fade = pow(fade, 1.5);                   // настраиваемое затухание
    
                gl_FragColor = v_color * texColor * fade;
            }
        """;

        ShaderProgram.pedantic = false;
        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);

        if (!shader.isCompiled()) {
            throw new GdxRuntimeException("Laser shader compile error:\n" + shader.getLog());
        }

        return shader;
    }

    public void turnOff() {
        light.setActive(false);
    }

    public void turnOn() {
        light.setActive(true);
    }

    public void update(Vector2 origin, float angleRad, Body ignoreBody) {
        from.set(origin);
        calcEnd(origin, angleRad, endFull);
        light.setPosition(origin);
        light.setDirection(angleRad * MathUtils.radiansToDegrees);
        castInternal(ignoreBody);
    }

    public void render(SpriteBatch batch) {
        if (!light.isActive()) return;

        batch.end();

        float dx = hitPoint.x - from.x;
        float dy = hitPoint.y - from.y;
        float len = (float) Math.hypot(dx, dy);
        float deg = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;

        laserShader.bind();
        laserShader.setUniformf("u_start", from.x, from.y);

        batch.setShader(laserShader);

        batch.begin();
        batch.setColor(1f, 0.2f, 0.2f, 1f);
        batch.draw(whitePixel,
                from.x, from.y - CORE_W * .5f,
                0,          CORE_W * .5f,
                len, CORE_W,
                1f, 1f,
                deg);

        batch.setShader(null); // вернуть стандартный шейдер
        batch.setColor(Color.WHITE); // вернуть цвет по умолчанию
        batch.end();
        batch.begin();
    }

    public void dispose() { light.remove(); }

    // ───── helpers ─────
    private void calcEnd(Vector2 o, float angRad, Vector2 out) {
        out.set(o.x + MathUtils.cos(angRad) * MAX_LEN,
                o.y + MathUtils.sin(angRad) * MAX_LEN);
    }

    private void castInternal(Body ignoreBody) {
        hitPoint.set(endFull);

        final float[] closest = {1f};

        world.rayCast((fxt, pt, nrm, frac) -> {
            if (ignoreBody != null && fxt.getBody() == ignoreBody) return 1f;
            if (fxt.isSensor()) return 1f;

            if (frac < closest[0]) {
                var gameObject = (GameObject) fxt.getBody().getUserData();
                GameObjectType type = gameObject.getType();
                if (type == GameObjectType.BULLET)
                    return 1f;
                hitPoint.set(pt);
                closest[0] = frac;
                // TODO: тут можно дать ИИ сделать уворот или урон нанести если лазер боевой
            }
            return frac;
        }, from, endFull);
    }
}