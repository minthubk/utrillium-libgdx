
package com.rsb.utrillium.views;

import java.util.ArrayList;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Files.FileType;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL10;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.ParticleEmitter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;
import com.rsb.utrillium.UTrilliumConst;
import com.rsb.utrillium.models.Bullet;
import com.rsb.utrillium.models.Player;
import com.rsb.utrillium.physics.CollisionHandler;
import com.rsb.utrillium.physics.MapBodyManager;
import com.rsb.utrillium.physics.PhysicsMaster;

public class GamePlayScreen extends BaseGameScreen {
	

	// game models
	private Player player;
	private Body playerBody;
	private ArrayList<Bullet> bullets = new ArrayList<Bullet>();
	
	// view fields
	private float cx;
	private float cy;
	
	private int currentScreenWidth;
	private int currentScreenHeight;

	private OrthogonalTiledMapRenderer renderer;
	private OrthographicCamera camera;
	private boolean renderPhysicsBodies=false;
	private boolean renderMapTiles=true;
	
	private ShapeRenderer shapeRenderer = new ShapeRenderer();
	
	private BitmapFont font = new BitmapFont (Gdx.files.internal("data/fonts/arial-15.fnt"),Gdx.files.internal("data/fonts/arial-15.png"), false, true);

	private SpriteBatch spriteBatch;
	private Texture planeTexture;
	private Sprite planeSprite;
	private Texture bulletTexture;
	private Sprite bulletSprite;	

	// particle fields
	private ParticleEffect effect;
	private int emitterIndex;
	private Array<ParticleEmitter> emitters;
	private int particleCount = 10;

	// audio
	private Sound bulletSound;
	
	// map fields
	private TiledMap map;
	private int mapWidth;
	private int mapHeight;
	private MapBodyManager mapBodyManager;
	private Box2DDebugRenderer debugRenderer;
	private Matrix4 debugMatrix;
	boolean mapLoaded=false;
	
	// physics fields
	private CollisionHandler collisionHandler;
	

	
	
	
	public GamePlayScreen (Game game) {
		super(game);
		
	}

	@Override
	public void show () {

		initParticles();
		// This method is called when screen becomes "current screen"
		// initialise everything for next call of render() method
		initPhysics();
		
		initCamera();
				
		initCurrentLevelMap();

		initPlayer(128f,128f);
		// load sprites
		initSprites();
		
		initSounds();
		
	}


	private void initPhysics() {
		// if world already exists detroy if first
		if(PhysicsMaster.physicsWorld != null) {
			PhysicsMaster.physicsWorld.dispose();
		}
		PhysicsMaster.physicsWorld = new World(new Vector2(0,0),true);
		
		collisionHandler = new CollisionHandler(this);
		PhysicsMaster.physicsWorld.setContactListener(collisionHandler);
	}
	
	private void initCamera() {
		updateScreenDimensions();

		camera = new OrthographicCamera();		
		camera.setToOrtho(false, currentScreenWidth, currentScreenHeight);
		camera.update();
	}

	private void initCurrentLevelMap() {
		Gdx.app.debug("UTrillium", "loading map");

		map = new TmxMapLoader().load("data/level01.tmx");
		renderer = new OrthogonalTiledMapRenderer(map, 1f);
		
		if(map == null) {
			String errorMsg = "Failed to load map ";
			Gdx.app.error("UTrillium.GameScreen", errorMsg);
		}
		
		// create Box2D physics world
		mapBodyManager = new MapBodyManager(PhysicsMaster.physicsWorld, 1/UTrilliumConst.TILE_WIDTH, "data/materials.xml", 0);
		mapBodyManager.createPhysics(map, "physics");
		
		debugRenderer=new Box2DDebugRenderer();
		
		this.mapLoaded = true;

		Gdx.app.debug("UTrillium", "map loaded");

	}
	
	
	private void initPlayer(float playerX, float playerY) {
		
		// create a new body for the player
		FixtureDef fixtureDef = new FixtureDef();
		fixtureDef.density = 1.0f;
		fixtureDef.friction = 1.0f;
		fixtureDef.restitution = 1.0f;
		
		
		Shape shape = PhysicsMaster.getRectangle(playerX, playerY, UTrilliumConst.PLAYER_WIDTH-20, UTrilliumConst.PLAYER_HEIGHT-20);;
		fixtureDef.shape = shape;
		
		BodyDef bodyDef = new BodyDef();
		bodyDef.type = BodyDef.BodyType.DynamicBody;			
		bodyDef.bullet=false;
		
		playerBody = PhysicsMaster.physicsWorld.createBody(bodyDef);
		playerBody.createFixture(fixtureDef);
		
		playerBody.setAngularDamping(5f);
		playerBody.setLinearDamping(1f);
		
		
		player = new Player(bullets,playerBody);
		
		playerBody.setUserData(player);
	}
	
	private void initSprites() {
		planeTexture = new Texture(Gdx.files.internal("data/sprites/plane.png")); 
		planeSprite = new Sprite(planeTexture);

		bulletTexture = new Texture(Gdx.files.internal("data/sprites/bullet.png")); 
		bulletSprite = new Sprite(bulletTexture);
	}

	private void initSounds() {
		bulletSound = Gdx.audio.newSound(Gdx.files.getFileHandle("data/sounds/shotgun.ogg", FileType.Internal));

	}
	
	private void initParticles() {

		effect = new ParticleEffect();
		effect.load(Gdx.files.internal("data/particle.p"), Gdx.files.internal("data"));
		effect.setPosition(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
		// Of course, a ParticleEffect is normally just used, without messing around with its emitters.
		emitters = new Array(effect.getEmitters());
		effect.getEmitters().clear();
		effect.getEmitters().add(emitters.get(0));
		
	}

	
	@Override
	public void render (float delta) {
		
		updateScreenDimensions();
		
		updateGameObjects(delta);
		
		processInput(delta);
		
		renderScreenObjects(delta);
		
	}

	private void renderScreenObjects(float delta) {
		// clear the screen
		Gdx.gl.glClearColor(0.7f, 0.7f, 1.0f, 1);
		Gdx.gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
			
		
		// let the camera follow the plane		
		moveCameraRelativeToPlayer();
		
		renderMap();
		
		renderBullets();

		renderPlane();

		renderParticles(delta);

		renderDebugBox2d();
		
		renderCameraCursor();
		
		renderDebugInfo(delta);
	}

	private void renderParticles(float delta) {
		spriteBatch.begin();
		effect.draw(spriteBatch, delta);
		spriteBatch.end();
		
	}

	private void renderBullets() {

		spriteBatch = renderer.getSpriteBatch();
		spriteBatch.begin();
		
		for (Bullet bullet : bullets) {

			bulletSprite.setPosition(bullet.position.x-UTrilliumConst.BULLET_WIDTH/2.0f, bullet.position.y-UTrilliumConst.BULLET_HEIGHT/2.0f);
			bulletSprite.setRotation(bullet.rotationInDegrees);
			bulletSprite.draw(spriteBatch);

		}
		
		
		spriteBatch.end();
		
	}

	private void updateGameObjects(float delta) {

		PhysicsMaster.update(delta);
				
		player.update(delta);
		
		/*
		// update bullets
		for (Bullet bullet : bullets) {

			bullet.update(delta);
		} 
		*/
	}

	private void updateScreenDimensions() {
		currentScreenWidth = Gdx.graphics.getWidth();
		currentScreenHeight = Gdx.graphics.getHeight();
		
		cx = currentScreenWidth/2;
		cy = currentScreenHeight/2;
	}

	private void renderDebugBox2d() {
		
		if(renderPhysicsBodies) {
			//Create a copy of camera projection matrix
		    debugMatrix=new Matrix4(camera.combined);
		 
			debugMatrix.scale(UTrilliumConst.TILE_WIDTH, UTrilliumConst.TILE_WIDTH, 1f);
			
			spriteBatch.begin();
	        //BoxObjectManager.GetWorld() gets the reference to Box2d World object
			debugRenderer.render(PhysicsMaster.physicsWorld, debugMatrix);
			spriteBatch.end();
		}
	}

	private void renderMap() {
		
		if(renderMapTiles) {
			
			renderer.render();
		}
	}

	
	private void moveCameraRelativeToPlayer() {
		camera.position.x = player.position.x;
		camera.position.y = player.position.y;
		camera.update();
		// set the tile map render view based on what the
		// camera sees and render the map
		renderer.setView(camera);
	}
	
	
	private void processInput(float deltaTime) {
		if (Gdx.input.isKeyPressed(Keys.ESCAPE)) {
			game.setScreen(new MainMenu(game));
		}
		
		// toggle map rendering
		if (Gdx.input.isKeyPressed(Keys.M)) {
			renderMapTiles = !renderMapTiles;
		}

		// toggle physics bodies rendering
		if (Gdx.input.isKeyPressed(Keys.B)) {
			renderPhysicsBodies = !renderPhysicsBodies;
		}
		
		if (Gdx.input.isKeyPressed(Keys.W) || Gdx.input.isKeyPressed(Keys.UP) ) {
			speedUp(deltaTime);
		}
		if (Gdx.input.isKeyPressed(Keys.S) || Gdx.input.isKeyPressed(Keys.DOWN)) {
			slowDown(deltaTime);
		}
		if (Gdx.input.isKeyPressed(Keys.A) || Gdx.input.isKeyPressed(Keys.LEFT)) {
			rotateLeft(deltaTime);
		}
		if (Gdx.input.isKeyPressed(Keys.D) || Gdx.input.isKeyPressed(Keys.RIGHT)) {
			rotateRight(deltaTime);
		}
		

	}
	
	
	private void speedUp(float deltaTime) {
		Vector2 force=new Vector2(10.0f*deltaTime,0);
		force.rotate((float) Math.toDegrees(playerBody.getTransform().getRotation()));
		playerBody.applyLinearImpulse(force, playerBody.getWorldCenter(),true);

	}

	private void slowDown(float deltaTime) {
		Vector2 force=new Vector2(-10.0f*deltaTime,0);
		force.rotate((float) Math.toDegrees(playerBody.getTransform().getRotation()));
		playerBody.applyLinearImpulse(force, playerBody.getWorldCenter(),true);
	}

	private void rotateLeft(float deltaTime) {
		playerBody.setAngularVelocity(5f);

	}

	private void rotateRight(float deltaTime) {
		playerBody.setAngularVelocity(-5f);
	}


	private void renderPlane() {

		spriteBatch = renderer.getSpriteBatch();
		spriteBatch.begin();
		
		planeSprite.setPosition(camera.position.x-planeTexture.getWidth()/2, camera.position.y-planeTexture.getHeight()/2);
		planeSprite.setRotation(player.rotationInDegrees);
		planeSprite.draw(spriteBatch);
		
		
		spriteBatch.end();
	}
	
	private void renderDebugInfo(float delta) {
		spriteBatch = renderer.getSpriteBatch();
		int fontX = (int) (camera.position.x - cx);
		int fontY = (int) (camera.position.y + cy);
		spriteBatch.begin();
		font.draw(spriteBatch, "FPS: " + Gdx.graphics.getFramesPerSecond(), fontX+10, fontY-20); 
		font.draw(spriteBatch, "Camera x: " + camera.position.x +" , Camera y: "+ camera.position.y+ " mapWidth: "+ mapWidth + " mapHeight: " +mapHeight+ " mapBodies:" + PhysicsMaster.physicsWorld.getBodyCount(), fontX+10, fontY-40); 
		font.draw(spriteBatch, "cx: " + cx +" , cy: "+ cy + " screenWidth: "+ currentScreenWidth + " screenHeight: " +currentScreenHeight + " delta:" + delta, fontX+10, fontY-60); 
		font.draw(spriteBatch, "collision count: " + PhysicsMaster.physicsWorld.getContactCount(), fontX+10, fontY-100); 
		//font.draw(spriteBatch, "playerX: " + player.position.x +" , playerY: "+ player.position.y + " playerBodyX: "+ player.physicsBody.getPosition().x + " playerBodyY: " +player.physicsBody.getPosition().y, fontX+10, fontY-80); 
		spriteBatch.end();
	}

	private void renderCameraCursor() {
		shapeRenderer.setProjectionMatrix(camera.combined);
		shapeRenderer.begin(ShapeType.Line);
		shapeRenderer.setColor(1, 1, 0, 1);
		shapeRenderer.line(camera.position.x, camera.position.y, camera.position.x+10, camera.position.y);
		shapeRenderer.line(camera.position.x, camera.position.y, camera.position.x-10, camera.position.y);
		shapeRenderer.line(camera.position.x, camera.position.y, camera.position.x, camera.position.y+10);
		shapeRenderer.line(camera.position.x, camera.position.y, camera.position.x, camera.position.y-10);
		shapeRenderer.end();
	}
	


	@Override
	public void hide () {
		Gdx.app.debug("UTrillium", "dispose game screen");
		renderer.dispose();
		shapeRenderer.dispose();
		mapBodyManager.destroyPhysics();
	}

	public void removeBullet(Bullet bullet) {

		bullets.remove(bullet);
	}

	public void addBulletExplosion(Vector2 position) {
			/*ParticleEmitter emitter = emitters.get(1);
			emitter.setPosition(position.x, position.y);
			particleCount += 10;
			System.out.println(particleCount);
			particleCount = Math.max(0, particleCount);
			if (particleCount > emitter.getMaxParticleCount()) emitter.setMaxParticleCount(particleCount * 2);
			emitter.getEmission().setHigh(particleCount / emitter.getLife().getHighMax() * 10);
			//effect.getEmitters().clear();
			effect.getEmitters().add(emitter);
			emitter.start();
		*/
		
		effect.setPosition(position.x, position.y);
		effect.start();
		
		bulletSound.play();
/*
		effect.setPosition( world.waterDropX, world.waterDropY);

		emitters = newArray(effect.getEmitters());

		effect.getEmitters().add(emitters.get(0));

		emitters.get(0).start();*/
	}
	
	
}
