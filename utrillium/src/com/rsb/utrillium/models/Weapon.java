package com.rsb.utrillium.models;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.rsb.utrillium.UTrilliumConst;

public class Weapon extends GameModel {

	
	public Weapon(float weaponOffsetX, float weaponOffsetY, PhysicsGameModel attachedTo, float reloadRate) {
		this.attachedTo = attachedTo;
		this.weaponOffsetX = weaponOffsetX + UTrilliumConst.BULLET_WIDTH/2f;
		this.weaponOffsetY = weaponOffsetY - UTrilliumConst.BULLET_HEIGHT/2f;
		this.reloadRate = reloadRate;
	}
	
	private float weaponOffsetX;
	private float weaponOffsetY;
	
	public boolean isLoaded=true;
	public boolean hasUnlimitedAmmo=true;
	public int currentAmmo=10;
	public int maxAmmoCapacity=10;

	public float reloadRate;
	public boolean isReloading=false;
	public float currentLoadTime;
	public PhysicsGameModel attachedTo;
	
	@Override
	public void update(float deltaTime) {

		if(isReloading) {
			currentLoadTime += deltaTime;
			
			if(currentLoadTime >= reloadRate) {
				isLoaded = true;
				isReloading = false;
				Gdx.app.log("UTrillium", "weapon loaded");
			}

		}
	}

	public Bullet fire() {

		// not currently loaded
		if(!isLoaded) {
			Gdx.app.log("UTrillium", "click, weapon not loaded");
			return null;
		}
		
		// any ammo left
		if(currentAmmo<=0 && hasUnlimitedAmmo==false) {
			Gdx.app.log("UTrillium", "click, out of ammo");
			return null;
		}
		
		// fire bullet
		
		// create bullet etc
		Bullet bullet = createBullet();
		
		
		//decrease ammo
		currentAmmo--;
		currentLoadTime=0;
		
		// start reloading
		if(currentAmmo>0 || hasUnlimitedAmmo ==true) {
			Gdx.app.log("UTrillium", "reloading started");
			isLoaded = false;
			isReloading=true;
		}
		
		Gdx.app.log("UTrillium", "weapon fired..");

		return bullet;
	}

	private Bullet createBullet() {

		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.density = 1.0f;
		fixtureDef.friction = 1.0f;
		fixtureDef.restitution = 1.0f;
		
		// calc offset of bullet position
		Vector2 offsetVector = new Vector2(weaponOffsetX,weaponOffsetY); 
		
		float rotationAngle = (float) Math.toDegrees(this.attachedTo.physicsBody.getTransform().getRotation());
		// rotate by plane direction
		offsetVector.rotate(rotationAngle);
		
		float x = attachedTo.position.x+offsetVector.x;
		float y = attachedTo.position.y+offsetVector.y;
		Shape shape = getBulletRectangle(x, y, UTrilliumConst.BULLET_WIDTH, UTrilliumConst.BULLET_HEIGHT);;
		fixtureDef.shape = shape;
		
		BodyDef bodyDef = new BodyDef();
		//bodyDef.type = BodyDef.BodyType.KinematicBody;			
		bodyDef.type = BodyDef.BodyType.DynamicBody;			
		bodyDef.bullet=true;
		Body body = PhysicsMaster.physicsWorld.createBody(bodyDef);
		Fixture tempFixture = body.createFixture(fixtureDef);
		
		// set direction
		// get velocity of plane
		Vector2 bulletVelocity=new Vector2(UTrilliumConst.BULLET_VELOCITY,0);
		// rotate by plane direction
		bulletVelocity.rotate(rotationAngle);
		
		Vector2 planeVelocity = this.attachedTo.physicsBody.getLinearVelocity();
		bulletVelocity.add(planeVelocity);
		
		body.setLinearVelocity(bulletVelocity);

		Bullet bullet = new Bullet(body, x,y);
		return bullet;

	}
	
	private Shape getBulletRectangle(float x, float y, float width, float height) {
		PolygonShape polygon = new PolygonShape();
		Vector2 size = new Vector2((x + (width * 0.5f)) * PhysicsMaster.unitsPerPixel,
								   (y + (height * 0.5f) ) * PhysicsMaster.unitsPerPixel);
		polygon.setAsBox((width * 0.5f) * PhysicsMaster.unitsPerPixel,
						 (height * 0.5f) * PhysicsMaster.unitsPerPixel,
						 size,
						 0.0f);
		return polygon;
	}
}
