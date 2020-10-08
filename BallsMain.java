import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapText;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.texture.Texture;
import com.jme3.util.SkyFactory;
import java.util.ArrayList;

/**
 * @author Kwame Duodu
 */
public class BallsMain extends SimpleApplication {
    public static void main(String args[]) {
    BallsMain app = new BallsMain();
    app.start();
  }
 
  // Prepare the Physics Application State 
  private BulletAppState bulletAppState;
 
  // Materials
  Material ball_mat, floor_mat, red_wall_mat, blue_wall_mat, 
           green_wall_mat, black_wall_mat;
 
  //Prepare geometries and physical nodes for walls and balls.
  private RigidBodyControl    ball_phy, floor_phy, red_wall_phy, 
                              blue_wall_phy, green_wall_phy, black_wall_phy;
  private static final Sphere sphere;
  private static final Box    floor, red_wall, blue_wall, green_wall, black_wall;
  private static  Node shootables;
  private static Geometry ball_geo, floor_geo, red_wall_geo, blue_wall_geo, 
                          green_wall_geo, black_wall_geo; 
  private static ArrayList list1, list2;
  
  static {
    // Initialize the ball geometry 
    sphere = new Sphere(32, 32, 0.4f, true, false);
    sphere.setTextureMode(TextureMode.Projected);
    //Initialize the floor geometry 
    floor = new Box(Vector3f.ZERO, 5f, 3f, 5f);
    //Initialize the walls geometry
    red_wall = new Box(Vector3f.ZERO, 5f, 5f, 0.5f);
    blue_wall = new Box(Vector3f.ZERO, 5f, 5f, 0.5f);
    green_wall = new Box(Vector3f.ZERO, 0.1f, 5f, 5f);
    black_wall = new Box(Vector3f.ZERO, 0.1f, 5f, 5f);
    list1 = new ArrayList();
    list2 = new ArrayList();
  }
 
  @Override
  public void simpleInitApp() {
    // Set up Physics Game 
    bulletAppState = new BulletAppState();
    stateManager.attach(bulletAppState);
     Texture west = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_west.jpg");
        Texture east = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_east.jpg");
        Texture north = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_north.jpg");
        Texture south = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_south.jpg");
        Texture up = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_up.jpg");
        Texture down = assetManager.loadTexture("Textures/Sky/Lagoon/lagoon_down.jpg");

      Spatial sky = SkyFactory.createSky(assetManager, west, east, north, south, up, down);
        rootNode.attachChild(sky);
        
    // This array is used to decide camera location
    float[] array = generateRandomLocation();
    
    // Cam location and direction will be selected randomly between 4 patterns.
    cam.setLocation(new Vector3f(array[0], 20f, array[1]));
    cam.lookAt(new Vector3f(array[2], 10, array[3]), Vector3f.UNIT_Y);
    // Add InputManager action: Left click triggers shooting
    inputManager.addMapping("shoot", 
            new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addListener(actionListener, "shoot");
    
    // Initialize the scene, materials, and physics space 
    initMaterials();
    initFloor();
    initRedWall();
    initBlueWall();
    initGreenWall();
    initBlackWall();
    initCrossHairs();
    
    // create a floor and four walls to shoot at
    shootables = new Node("Shootables");
    rootNode.attachChild(shootables);
    shootables.attachChild(floor_geo);
    shootables.attachChild(red_wall_geo);
    shootables.attachChild(blue_wall_geo);
    shootables.attachChild(green_wall_geo);
    shootables.attachChild(black_wall_geo);
  }
 
  // Every time the shoot action is triggered, a new ball is produced.
  
  private ActionListener actionListener = new ActionListener() {
    public void onAction(String name, boolean keyPressed, float tpf) {
      initCrossHairs();
      if (name.equals("shoot") && !keyPressed) {
        makeBall();
        // Reset results list.
        CollisionResults results = new CollisionResults();
        // Aim the ray from cam loc to cam direction.
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());
        // Collect intersections between Ray and Shootables in results list.
        shootables.collideWith(ray, results);
        // Print the results
        for (int i = 0; i < results.size(); i++) {
          float dist = results.getCollision(i).getDistance();
          Vector3f pt = results.getCollision(i).getContactPoint();
          String hit = results.getCollision(i).getGeometry().getName();
          // Display a position of a shpere on collision
          // For each hit, we know distance, impact point, name of geometry.
          guiNode.detachAllChildren();
          guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
          BitmapText text = new BitmapText(guiFont, false);
          text.setSize(guiFont.getCharSet().getRenderedSize());
          text.setText("Ball hits " + hit + " at " + pt);
          text.setLocalTranslation(250, text.getLineHeight(), 0);
          guiNode.attachChild(text);
          // Store 3D vector positions to an array list and show them
          list1.add(pt);
          System.out.println("(x, y, z) = " + list1);
        }
        if (results.size() > 0) {
            CollisionResult closest = results.getClosestCollision();
            ball_geo.setLocalTranslation(closest.getContactPoint());
            rootNode.attachChild(ball_geo);
        } else {
            rootNode.detachChild(ball_geo);
        }            
      }
    }
  };
 
   // Initialize the materials used in this scene.
   public void initMaterials() {
    ball_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    ball_mat.setColor("Color", ColorRGBA.White);
    
    floor_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    floor_mat.setColor("Color", ColorRGBA.randomColor());
    floor_mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
    
    red_wall_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    red_wall_mat.setColor("Color", ColorRGBA.Red);
    red_wall_mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
    
    blue_wall_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    blue_wall_mat.setColor("Color", ColorRGBA.Blue);
    blue_wall_mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
    
    green_wall_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    green_wall_mat.setColor("Color", ColorRGBA.Green);
    green_wall_mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
    
    black_wall_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    black_wall_mat.setColor("Color", ColorRGBA.Black);
    black_wall_mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
   }
  
   //Make a solid floor
   public void initFloor() {
    floor_geo = new Geometry("floor", floor);
    floor_geo.setMaterial(floor_mat);
    //make the floor translucent
    floor_geo.setQueueBucket(RenderQueue.Bucket.Translucent);
    floor_geo.setLocalTranslation(0, -0.2f, 0);
    this.rootNode.attachChild(floor_geo);
    /* Make the floor physical with mass 0.0f! */
    floor_phy = new RigidBodyControl(0.0f);
    floor_geo.addControl(floor_phy);
    bulletAppState.getPhysicsSpace().add(floor_phy);
   }

   // Make solid walls.
   public void initRedWall() {
    red_wall_geo = new Geometry("red wall", red_wall);
    red_wall_geo.setMaterial(red_wall_mat);
    //make the wall translucent
    red_wall_geo.setQueueBucket(RenderQueue.Bucket.Translucent);
    red_wall_geo.setLocalTranslation(0, 5.9f, 5.1f);
    this.rootNode.attachChild(red_wall_geo);
    /* Make the wall physical with mass 0.0f! */
    red_wall_phy = new RigidBodyControl(0.0f);
    red_wall_geo.addControl(red_wall_phy);
    bulletAppState.getPhysicsSpace().add(red_wall_phy);
   }
   
   public void initGreenWall() {
    green_wall_geo = new Geometry("green wall", green_wall);
    green_wall_geo.setMaterial(green_wall_mat);
    //make the wall translucent
    green_wall_geo.setQueueBucket(RenderQueue.Bucket.Translucent);
    green_wall_geo.setLocalTranslation(-5.1f, 5.9f, 0);
    this.rootNode.attachChild(green_wall_geo);
    green_wall_phy = new RigidBodyControl(0.0f);
    green_wall_geo.addControl(green_wall_phy);
    bulletAppState.getPhysicsSpace().add(green_wall_phy);
   }
   
   public void initBlueWall() {
    blue_wall_geo = new Geometry("blue wall", blue_wall);
    blue_wall_geo.setMaterial(blue_wall_mat);
    //make the wall translucent
    blue_wall_geo.setQueueBucket(RenderQueue.Bucket.Translucent);
    blue_wall_geo.setLocalTranslation(0, 5.9f, -5.1f);
    this.rootNode.attachChild(blue_wall_geo);
    blue_wall_phy = new RigidBodyControl(0.0f);
    blue_wall_geo.addControl(blue_wall_phy);
    bulletAppState.getPhysicsSpace().add(blue_wall_phy);
   } 
   
   public void initBlackWall() {
    black_wall_geo = new Geometry("yellow wall", black_wall);
    black_wall_geo.setMaterial(black_wall_mat);
    //make the wall translucent
    black_wall_geo.setQueueBucket(RenderQueue.Bucket.Translucent);
    black_wall_geo.setLocalTranslation(5.1f, 5.9f, 0);
    this.rootNode.attachChild(black_wall_geo);
    black_wall_phy = new RigidBodyControl(0.0f);
    black_wall_geo.addControl(black_wall_phy);
    bulletAppState.getPhysicsSpace().add(black_wall_phy);
   }
 
  // This method creates one individual physical ball.
   public void makeBall() {
    /** Create a ball geometry. */
    ball_geo = new Geometry("ball", sphere);
    ball_geo.setMaterial(ball_mat);
    shootables.attachChild(ball_geo);
    /** Position the ball  */
    ball_geo.setLocalTranslation(cam.getLocation());
    /** Make the ball physcial with a mass > 0.0f */
    ball_phy = new RigidBodyControl(1f);
    /** Add physical ball to physics space. */
    ball_geo.addControl(ball_phy);
    bulletAppState.getPhysicsSpace().add(ball_phy);
    /** Accelerate the physcial ball to shoot it. */
    int velocity = generateRandomVelocity();
    ball_phy.setLinearVelocity(cam.getDirection().mult(velocity));
    // Store linear velocities to an array list and show them
    list2.add(velocity);
    System.out.println("Velocity is " + list2);
    /** Set gravity on y-axis. */
    ball_phy.setGravity(new Vector3f(0, -5, 0));
  }
   
    // Generate a random velocity from 20 to 80.
    private int generateRandomVelocity() {
        int v = (int) Math.ceil(Math.random()*61)+20;
        return v;
    }
    
    // Generate a random camera location between 4
    private float[] generateRandomLocation() {
        float[] a0 = {5f, 10f, 1f, -1f};
        float[] a1 = {10f, -10f, -1f, -1f};
        float[] a2 = {-5f, -10f, -1f, 1f};
        float[] a3 = {-10f, 5f, 1f, 1f};
        int n = (int) Math.floor(Math.random()*4);
        switch (n) {
            case 0: return a0;
            case 1: return a1;
            case 2: return a2;
            case 3: return a3;
            default: return a0;
        }
    }
 
    // A plus sign used as crosshairs to help the player with aiming
    protected void initCrossHairs() {
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("+");        // fake crosshairs :)
        ch.setLocalTranslation( // center
            settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
            settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
    } 
}