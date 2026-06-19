package com.gungame.world.objects.phisical;

import box2dLight.ConeLight;
import box2dLight.PointLight;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import com.gungame.assets.SoundManager;
import com.gungame.world.GameWorld;
import com.gungame.world.GameWorldConfig;
import com.gungame.world.collision.CollisionCategory;
import com.gungame.world.collision.CollisionFilters;
import com.gungame.world.explosion.ExplosionUtils;
import com.gungame.world.objects.meta.CustomObjectInitializationConfig;
import com.gungame.world.objects.meta.DynamicVisibleGameObject;
import com.gungame.world.objects.meta.GameObject;
import com.gungame.world.objects.meta.GameObjectType;
import com.gungame.world.objects.weapon.GrenadeThrower;
import com.gungame.world.objects.weapon.Gun;
import com.gungame.world.objects.weapon.GunData;
import com.gungame.world.objects.weapon.Laser;
import lombok.Getter;
import lombok.NonNull;


public class Hero extends DynamicVisibleGameObject {
    public static final int MAX_HEALTH = 100;
    public static final float MAX_STAMINA = 100f;
    public static final float FIRE_POSITION_DX = 0.7f;
    public static final float FIRE_POSITION_DY = 0.22f;

    private static final float MAX_STAMINA_REGEN_SPEED = 0.025f;
    private static final float BOX_COLLISION_BODY_CIRCLE_RADIUS = .07f;
    private static final int DEFAULT_DAMPING = 5;
    private static final int DAMPING_RESTORE_TIME = 100;
    private @Getter final GrenadeThrower grenadeThrower = new GrenadeThrower(this);

    private float xScale;
    private float yScale;
    private final PointLight playerLight;
    private ConeLight fireLight;
    private final Laser laser;
    private @NonNull MovingMode movingMode = MovingMode.STANDING;
    private @Getter float stamina = MAX_STAMINA;
    private long lastStaminaUsage = System.currentTimeMillis();
    private long lastStaminaRegen = lastStaminaUsage;
    private long lastMovingModeChange = lastStaminaRegen;
    private boolean isAbleToRun;
    private @Getter int health = MAX_HEALTH;
    private final Gun[] gun = {new Gun(GunData.RIFLE), new Gun(GunData.SMG), new Gun(GunData.SHOTGUN)};
    private int currentWeapon = 0;
    private long timeWhenDampingTimeChanged = 0;
    // Пул для обхода всех Body при поиске ближайшего FirePoint.
    // Выделяется один раз, переиспользуется через clear() — avoids GC per call.
    private final Array<Body> hidesBoxBodyPool = new Array<>();

    public Hero(GameWorld gameWorld, GameObjectType type, Body body, Sprite sprite) {
        super(gameWorld, type, body, sprite);
        body.setUserData(this);

        // Свет от персонажа
        playerLight = new PointLight(getWorld().getRayHandler(), 512);
        playerLight.attachToBody(getBody(), 0.25f, 0.5f);
        playerLight.setIgnoreAttachedBody(true);
        playerLight.setDistance(12f);
        playerLight.setSoft(true);
        playerLight.setSoftnessLength(7.5f);
        playerLight.setContactFilter(CollisionFilters.HIGH_LIGHT_CONTACT_FILTER);
        playerLight.setColor(new Color(0f, 0f, 0f, 1f));

        // лазер
        laser = new Laser(gameWorld, getFirePosition(), getAngle(), body);
    }

    public void takeDamage(int damage) {
        if (isToDestroy()) {
            return;
        }
        SoundManager.play(SoundManager.Sfx.DAMAGE);
        health -= damage;
        if (health <= 0) {
            death();
            health = 0; //что бы не уходило в минус
        }
    }

    public void death() {
        laser.turnOff();
        SoundManager.play(SoundManager.Sfx.DEATH);
        markForDestroy();
    }

    public void fire() {
        if (isToDestroy()) {
            return;
        }

        var bullets = getCurrentGun().fire();
        if (bullets == null) {
            return;
        }

        Vector2 firePosition = getFirePosition();
        var hidesBox = hidesBox(firePosition);
        CustomObjectInitializationConfig customInitConfig = new CustomObjectInitializationConfig();
        if (hidesBox != null) {
            customInitConfig.setGroupIndex(hidesBox.getGroupIndex());
        } else {
            customInitConfig.setGroupIndex(bullets.getFirst().shotID());
        }

        GameWorld world = getWorld();
        var bulletFactory = world.getPhysicalObjectFactoryManager().getBulletFactory();
        float bodyAngle = getAngle();
        for (var bulletData : bullets) {
            float angle = bodyAngle + bulletData.deviation();
            bulletFactory.create(firePosition, angle * MathUtils.radiansToDegrees, customInitConfig,
                    bullet -> {
                bullet.setVelocity(MathUtils.cos(angle) * bulletData.speed(), MathUtils.sin(angle) * bulletData.speed());
                bullet.setDamage(bulletData.damage());

                if (getCurrentGun().hasHeavyBullets()) {
                    // увеличиваем плотность в 100 раз
                    Fixture fixture = bullet.getBody().getFixtureList().first();
                    fixture.setDensity(fixture.getDensity() * 100);
                    bullet.getBody().resetMassData();

                    world.getCameraShaker().shake(3f, .5f);
                } else {
                    world.getCameraShaker().shake(1f, .2f);
                }
            });
        }

        fireLight = new ConeLight(world.getRayHandler(), 64, Color.RED, 10,
                firePosition.x, firePosition.y, bodyAngle  * MathUtils.radiansToDegrees, 20);
        fireLight.setContactFilter(CollisionFilters.LOW_LIGHT_CONTACT_FILTER);
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
        if (isToDestroy()) {
            return;
        }
        getCurrentGun().reloadStart();
    }

    public void switchWeapon() {
        if (isToDestroy()) {
            return;
        }
        if(!getCurrentGun().isReloading()) {
            currentWeapon = (currentWeapon + 1) % gun.length;
        }
    }

    public void setWeapon(int id) {
        if (isToDestroy()) {
            return;
        }
        if(!getCurrentGun().isReloading()) {
            currentWeapon = id;
        }
    }

    private FirePoint hidesBox(Vector2 pos) {
        hidesBoxBodyPool.clear();
        getWorld().getPhysicsWorld().getBodies(hidesBoxBodyPool);

        FirePoint nearestFirePoint = null;
        float nearestDistance = Float.MAX_VALUE;
        final float minDistance = Math.min(xScale, yScale) * BOX_COLLISION_BODY_CIRCLE_RADIUS * 5;
        // поворот не учитывается, так что пока для небольшого количества объектов норм

        for (var body : hidesBoxBodyPool) {
            var gameObject = (GameObject) body.getUserData();
            if (gameObject instanceof FirePoint firePoint) {
                var distance = firePoint.getPosition().dst(pos);
                if (distance < minDistance && distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestFirePoint = firePoint;
                }
            }
        }

        return nearestFirePoint;
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

        if (isToDestroy()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (stamina < MAX_STAMINA && movingMode.getStaminaCost() == 0) {
            long delta = now - lastStaminaRegen;
            float staminaRegenSpeed = MAX_STAMINA_REGEN_SPEED * movingMode.getStaminaRegenSpeed();
            stamina = Math.min(MAX_STAMINA, stamina + delta * staminaRegenSpeed);
            lastStaminaRegen = now;
        }

        Gun currentGun = getCurrentGun();
        currentGun.checkReloadingComplete(now);
        if (fireLight != null && now - currentGun.getReloadingTimer() > 10) {
            fireLight.remove();
            fireLight = null;
        }

        if (movingMode.getStaminaCost() != 0) {
            lastStaminaRegen = now;
        }
        timeWhenDampingTimeChanged = ExplosionUtils.checkDamping(
                body, now, DEFAULT_DAMPING, timeWhenDampingTimeChanged, DAMPING_RESTORE_TIME);

        laser.update(getFirePosition(), getAngle(), getBody());
    }

    public void tryChangeMovingMode(MovingMode newMovingMode) {
        if (isToDestroy()) {
            return;
        }
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
                SoundManager.play(SoundManager.Sfx.DASH);
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
        if (x * x + y * y < .1f || body.getLinearDamping() != DEFAULT_DAMPING) {
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
        if (isToDestroy()) {
            return 0f;
        }
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
        filter.categoryBits = CollisionCategory.HEIGHT_OBJECTS.getBits();
        filter.maskBits = CollisionCategory.MICRO_OBJECTS.getBits();
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
        fixtureDef.filter.categoryBits = CollisionCategory.ALL_PHYSICAL.getBits();
        fixtureDef.filter.maskBits = CollisionCategory.ALL_PHYSICAL.getBits();

        var circleShape = new CircleShape();
        circleShape.setPosition(body.getLocalCenter());
        circleShape.setRadius(Math.min(xScale, yScale) * BOX_COLLISION_BODY_CIRCLE_RADIUS);
        fixtureDef.shape = circleShape;
        body.createFixture(fixtureDef);
        circleShape.dispose();
    }

    @Override
    public int getDrawLevel() {
        return 1;
    }

    @Override
    public void draw(SpriteBatch batch) {
        super.draw(batch);
        laser.render(batch);
        grenadeThrower.render(batch, getFirePosition(),getAngle());
    }

    @Override
    public void dispose() {
        // Light.remove() внутри box2dLight уже disposes mesh.
        // Вызывать dispose() после remove() — double free crash.
        // Достаточно remove(): лайт открепляется от RayHandler,
        // а rayHandler.dispose() почистит оставшиеся.
        playerLight.remove();
        if (fireLight != null) {
            fireLight.remove();
        }
        laser.dispose();
    }
}
