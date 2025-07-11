package com.gungame.world.objects.phisical;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Array;
import com.gungame.world.GameWorld;
import com.gungame.world.GameWorldConfig;
import com.gungame.world.collision.CollisionCategory;
import com.gungame.world.objects.meta.CustomObjectInitializationConfig;
import com.gungame.world.objects.meta.DynamicVisibleGameObject;
import com.gungame.world.objects.meta.GameObjectType;
import com.gungame.world.objects.weapon.*;
import lombok.Getter;
import lombok.NonNull;

import java.util.Random;
import java.util.stream.Stream;

import static com.badlogic.gdx.math.MathUtils.random;

public class Hero extends DynamicVisibleGameObject {
    public static final float MAX_STAMINA = 100f;
    public static final float FIRE_POSITION_DX = 0.7f;
    public static final float FIRE_POSITION_DY = 0.15f;

    private static final float MAX_STAMINA_REGEN_SPEED = 0.025f;
    private static final float BOX_COLLISION_BODY_CIRCLE_RADIUS = .15f;
    private final Sound[] damageSounds = {
            Gdx.audio.newSound(Gdx.files.internal("sound/damage1.wav")),
            Gdx.audio.newSound(Gdx.files.internal("sound/damage2.wav"))
    };
    private final Sound[] dashSounds = {
            Gdx.audio.newSound(Gdx.files.internal("sound/dash1.wav")),
            Gdx.audio.newSound(Gdx.files.internal("sound/dash2.wav"))
    };
    private final Sound deathSound = Gdx.audio.newSound(Gdx.files.internal("sound/death.wav"));

    private float xScale;
    private float yScale;
    private @NonNull MovingMode movingMode = MovingMode.STANDING;
    private @Getter float stamina = MAX_STAMINA;
    private long lastStaminaUsage = System.currentTimeMillis();
    private long lastStaminaRegen = lastStaminaUsage;
    private long lastMovingModeChange = lastStaminaRegen;
    private boolean isAbleToRun;
    private @Getter int health = 100;
    private final Gun[] gun = {new Gun(GunData.RIFLE), new Gun(GunData.SMG), new Gun(GunData.SHOTGUN)};
    private int currentWeapon = 0;

    public Hero(GameWorld gameWorld, GameObjectType type, Body body, Sprite sprite) {
        super(gameWorld, type, body, sprite);
    }

    public void takeDamage(int damage) {
        damageSounds[random.nextInt(damageSounds.length)].play();
        health -= damage;
        if (health <= 0) {
            death();
            health = 0; //что бы не уходило в минус
        }
    }

    public void death() {
        deathSound.play();
        markForDestroy();
    }

    public void fire() {
        var bullets = getCurrentGun().fire();
        if (bullets == null) {
            return;
        }

        Vector2 firePosition = getFirePosition();
        var hidesBox = hidesBox(firePosition);
        CustomObjectInitializationConfig customInitConfig;
        customInitConfig = new CustomObjectInitializationConfig();
        if (hidesBox != null) {
            customInitConfig.setGroupIndex(hidesBox.getGroupIndex());
        } else {
            customInitConfig.setGroupIndex(bullets.getFirst().shotID());
        }

        var bulletFactory = getWorld().getPhysicalObjectFactoryManager().getBulletFactory();
        for (var bulletData : bullets) {
            float angle = getAngle() + bulletData.deviation();
            bulletFactory.create(firePosition, angle * MathUtils.radiansToDegrees, customInitConfig,
                    bullet -> {
                bullet.setVelocity(MathUtils.cos(angle) * bulletData.speed() , MathUtils.sin(angle) * bulletData.speed());
                bullet.setDamage(bulletData.damage());
            });
        }
    }

    /**
     * Расчёт места появления пули
     */
    public Vector2 getFirePosition() {
        float totalOffsetX = FIRE_POSITION_DX * xScale;
        float totalOffsetY = FIRE_POSITION_DY * yScale;

        // Вращаем вокруг центра массы
        float cos = MathUtils.cos(body.getAngle());
        float sin = MathUtils.sin(body.getAngle());
        float rotatedX = totalOffsetX * cos - totalOffsetY * sin;
        float rotatedY = totalOffsetX * sin + totalOffsetY * cos;

        float fireX = body.getPosition().x + rotatedX;
        float fireY = body.getPosition().y + rotatedY;
        return new Vector2(fireX, fireY);
    }

    public void reloadStart() {
        getCurrentGun().reloadStart();
    }

    public void switchWeapon() {
        if(!getCurrentGun().isReloading()) {
            currentWeapon = (currentWeapon + 1) % gun.length;
        }
    }

    public void setWeapon(int id) {
        if(!getCurrentGun().isReloading()) {
            currentWeapon = id;
        }
    }

    private Box hidesBox(Vector2 pos) {
        var arr = new Array<Body>();
        getWorld().getPhisicsWorld().getBodies(arr);

        Box nearestBox = null;
        float nearestDistance = Float.MAX_VALUE;
        final float minDistance = Math.min(xScale, yScale) * BOX_COLLISION_BODY_CIRCLE_RADIUS * 5;
        // поворот не учитывается, так что пока для небольшого количества объектов норм

        for (var body : arr) {
            var userData = body.getUserData();
            if (userData instanceof Box box) {
                var distance = box.getPosition().dst(pos);
                if (distance < minDistance && distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestBox = box;
                }
            }
        }

        return nearestBox;
    }

    public boolean tryUseStamina(float staminaToUse) {
        if (staminaToUse > this.stamina) {
            return false;
        }
        this.stamina -= staminaToUse;
        this.lastStaminaUsage = System.currentTimeMillis();
        return true;
    }

    @Override
    public void update() {
        super.update();

        long now = System.currentTimeMillis();
        if (stamina < MAX_STAMINA && movingMode.getStaminaCost() == 0) {
            long delta = now - lastStaminaRegen;
            float staminaRegenSpeed = MAX_STAMINA_REGEN_SPEED * movingMode.getStaminaRegenSpeed();
            stamina = Math.min(MAX_STAMINA, stamina + delta * staminaRegenSpeed);
            lastStaminaRegen = now;
        }
        getCurrentGun().isReloadingComplete(now);
        if (movingMode.getStaminaCost() != 0) {
            lastStaminaRegen = now;
        }
    }

    public void tryChangeMovingMode(MovingMode newMovingMode) {
        if (stamina > 20) {
            isAbleToRun = true;
        }
        if (movingMode == newMovingMode || !isAbleToRun) {
            return;  // ни чего менять не требуется
        }
        if (stamina < 1) {
            isAbleToRun = false;
        }

        var now = System.currentTimeMillis();
        if (now - lastMovingModeChange < newMovingMode.getMinDuration()) {
            return;
        }
        if (tryUseStamina(newMovingMode.getStaminaCost() * newMovingMode.getMinDuration())) {
            movingMode = newMovingMode;
            lastMovingModeChange = now;
            if (movingMode == MovingMode.JUMPING) {
                dashSounds[random.nextInt(dashSounds.length)].play();
            }
        }
    }

    /**
     * Применяет к персонажу силу для его передвижения.
     *
     * @param x направление (часть вектора)
     * @param y направление (часть вектора)
     * @return true если сила применена
     */
    public boolean move(float x, float y) {
        if (x * x + y * y < .1f) {
            return false;  // может быть небольшая погрешность
        }

        long now = System.currentTimeMillis();
        long delta = now - lastStaminaUsage;
        if (movingMode.getMaxDuration() > 0 && delta > movingMode.getMaxDuration()
                || (delta > movingMode.getMinDuration()
                && !tryUseStamina(movingMode.getStaminaCost() * delta))) {
            movingMode = MovingMode.NORMAL;
            lastMovingModeChange = now;
        }

        float impulseX = getImpulse(x, movingMode);
        float impulseY = getImpulse(y, movingMode);
        applyImpulse(impulseX, impulseY);
        return true;
    }

    private float getImpulse(float acceleration, MovingMode movingMode) {
        float potentialResult = GameWorldConfig.HERO_ACCELERATION * acceleration;
        if (movingMode == MovingMode.RUNNING) {
            potentialResult *= GameWorldConfig.HERO_RUNNING_ACCELERATION_SCALE;
        } else if (movingMode == MovingMode.JUMPING) {
            potentialResult *= GameWorldConfig.HERO_JUMPING_ACCELERATION_SCALE;
        }
        return potentialResult;
    }

    public final Gun getCurrentGun() {
        return gun[currentWeapon];
    }

    public float getBodyRadius() {
        return Math.min(xScale, yScale) * BOX_COLLISION_BODY_CIRCLE_RADIUS * 5;
    }

    @Override
    public void setupCollisionFilter(Filter filter) {
        filter.categoryBits = CollisionCategory.HEIGHT_OBJECTS.getBitMask();
        filter.maskBits = CollisionCategory.HEIGHT_OBJECTS.getBitMask();
    }

    @Override
    public void postConstruct() {
        super.postConstruct();

        var defaultMassData = getWorld().getPhysicalObjectFactoryManager()
                .getHeroFactory()
                .getObjectMetadata()
                .getMassData();
        var massCenter = body.getMassData().center;
        xScale = massCenter.x / defaultMassData.center.x;
        yScale = massCenter.y / defaultMassData.center.y;

        var fixtureDef = new FixtureDef();
        fixtureDef.density = 100f;
        fixtureDef.friction = 1f;
        fixtureDef.filter.categoryBits = CollisionCategory.ALL.getBitMask();
        fixtureDef.filter.maskBits = CollisionCategory.ALL.getBitMask();

        var circleShape = new CircleShape();
        circleShape.setPosition(body.getLocalCenter());
        circleShape.setRadius(Math.min(xScale, yScale) * BOX_COLLISION_BODY_CIRCLE_RADIUS);
        fixtureDef.shape = circleShape;
        body.createFixture(fixtureDef);
    }

    @Override
    public int getDrawLevel() {
        return 1;
    }

    @Override
    public void dispose() {
        Stream.concat(Stream.of(dashSounds), Stream.of(deathSound)).forEach(Sound::dispose);
    }
}
