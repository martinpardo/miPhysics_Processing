package miPhysics.Engine;

import java.util.*;
import java.util.concurrent.locks.*;
import java.lang.Math;

import miPhysics.Control.ParamController;

import processing.core.*;

/**
 * The global physics context, inside which we can define physical models.
 */
public class PhysicsContext {

	// myParent is a reference to the parent sketch
	PApplet myParent;

	private Lock m_lock;

	/* The simulation rate (mono rate only) */
	private int simRate;
	/* The processing sketch display rate */
	private int displayRate;

	private double simDisplayFactor;
	private int nbStepsToSim;
	private double residue;

	private paramSystem m_unit_system;

	private Map<String, ArrayList<Mass>> m_mass_subsets;
	private Map<String, ArrayList<Interaction>> m_int_subsets;

	private Medium m_medium = new Medium();

	private velUnit m_velUnits = velUnit.PER_SEC;

	private int m_errorCode = 0;

	private PhyModel m_topLevelModel = new PhyModel("top", m_medium);

	private CollisionEngine m_colEng = new CollisionEngine();

	private Map<String, ParamController> param_controllers = new HashMap<>();


	/* Library version */
	public final static String VERSION = "##library.prettyVersion##";

	public PhysicsContext(int sRate, int displayRate, paramSystem sys) {

		m_mass_subsets = new HashMap<>();
		m_int_subsets = new HashMap<>();


		m_unit_system = sys;

		if (sRate > 0)
			setSimRate(sRate);
		else {
			System.out.println("Invalid simulation Rate: defaulting to 50 Hz");
			setSimRate(50);
		}

		this.displayRate = displayRate;
		this.residue = 0;

		this.calculateSimDisplayFactor();

		m_lock = new ReentrantLock();

		System.out.println("Physical Model Class Initialised");
	}

	/**
	 * Constructor without specifying the parameter system (defaults to algo
	 * parameters)
	 *
	 * @param sRate
	 *            the physics sample rate
	 *
	 */
	public PhysicsContext(int sRate, int displayRate) {
		this(sRate, displayRate, paramSystem.ALGO_UNITS);
		System.out.println("No specified display Rate: defaulting to 30 FPS");
	}

	/**
	 * Constructor without specifying the sketch display rate (defaults to 30 FPS),
	 * or the parameter system (defaults to algo parameters)
	 *
	 * @param sRate
	 *            the physics sample rate
	 *
	 */
	public PhysicsContext(int sRate) {
		this(sRate, 30, paramSystem.ALGO_UNITS);
		System.out.println("No specified display Rate: defaulting to 30 FPS");
	}

	/**
	 * Constructor without specifying the sketch display rate (defaults to 30 FPS).
	 *
	 * @param sRate
	 *            the physics sample rate
	 *
	 */
	public PhysicsContext(int sRate, paramSystem sys) {
		this(sRate, 30, sys);
		System.out.println("No specified display Rate: defaulting to 30 FPS");
	}

	/**
	 * Delete all modules in the model and start from scratch.
	 */
	public void clearModel() {
		m_topLevelModel.clear();
	}


	private void calculateSimDisplayFactor() {
		simDisplayFactor = (float) simRate / (float) displayRate;
	}

	/*************************************************/
	/* Some utility functions for the class */
	/*************************************************/

	/**
	 * Get the simulation's sample rate.
	 *
	 * @return the simulation rate
	 */
	public int getSimRate() {
		return simRate;
	}

	/**
	 * Set the simulation's sample rate.
	 *
	 * @param rate
	 *            the rate to set the simulation to (physics frame-per-second).
	 */
	public void setSimRate(int rate) {
		simRate = rate;
		this.calculateSimDisplayFactor();
	}


	public void setVelUnit(velUnit v){
		this.m_velUnits = v;
	}

	public int getErrorCode(){
		return m_errorCode;
	}

	/**
	 * Get the top level physical model of the physics context.
	 * @return the model.
	 */
	public PhyModel mdl(){
		return m_topLevelModel;
	}

	/**
	 * Get the collision engine.
	 * @return the engine.
	 */
	public CollisionEngine colEngine(){return m_colEng;}

	/*************************************************/
	/* Compute simulation steps */
	/*************************************************/

	/**
	 * Run the physics simulation (call once every draw method). Automatically
	 * computes the correct number of steps depending on the simulation rate /
	 * display rate ratio. Should be called once the model creation is finished and
	 * the init() method has been called.
	 *
	 */
	public void computeScene() {
		double floatFrames = this.simDisplayFactor + this.residue;
		int nbSteps = (int) Math.floor(floatFrames);
		this.residue = floatFrames - (double) nbSteps;

		this.computeNSteps(nbSteps);
	}

	/**
	 * Explicitly compute N steps of the physical simulation. Should be called once
	 * the model creation is finished and the init() method has been called.
	 *
	 * @param N
	 *            number of steps to compute.
	 */
	public void computeNSteps(int N) {
		synchronized (m_lock) {
			for (int j = 0; j < N; j++) {

				if(!param_controllers.isEmpty())
					param_controllers.forEach((k,v)-> v.updateParams());

				m_topLevelModel.compute();
				// TODO: in and out updates should occur AFTER collision calculations!
				m_colEng.runCollisions();

			}
		}
	}

	/**
	 * Compute a single step of the physical simulation. Should be called once the
	 * model creation is finished and the init() method has been called.
	 */
	public void computeSingleStep() {
		computeNSteps(1);
	}

	public Lock getLock(){
		return m_lock;
	}


	public Medium getGlobalMedium(){
		return this.m_medium;
	}

	public void setGlobalFriction(double d){
		this.m_medium.setMediumFriction(d);
	}

	public void setGlobalGravity(Vect3D g){
		this.m_medium.setGravity(g);
	}

	public void setGlobalGravity(double gx, double gy, double gz){
		this.m_medium.setGravity(new Vect3D(gx, gy, gz));
	}

	public double getGlobalFriction(){
		return this.m_medium.getMediumFriction();
	}

	public Vect3D getGlobalGravity(){
		return this.m_medium.getGravity();
	}



	/**
	 * Initialise the physical model once all the modules have been created.
	 */
	public void init() {
		m_topLevelModel.init();
		System.out.println("Initialisation of the physical model: ");
		System.out.println("Nb of Mats in model: " + m_topLevelModel.numberOfMassTypes());
		System.out.println("Nb of Links in model: " + m_topLevelModel.numberOfInterTypes());
		System.out.println("Finished model init.\n");
	}

	public void addParamController(String name,String subsetName,String paramName,float rampTime)
	{
		param_controllers.put(name,new ParamController(this,rampTime,subsetName,paramName));
	}

	public ParamController getParamController(String name) {return param_controllers.get(name);}


	/**
	 * Create an empty Mass module subset item. Module references will be associated to
	 * this specific key later.
	 *
	 * @param name
	 *            the identifier for this subset.
	 * @return 0 if success, -1 otherwise.
	 */
	public int createMassSubset(String name) {
		if (!this.m_mass_subsets.containsKey(name)) {
			this.m_mass_subsets.put(name, new ArrayList<>());
			return 0;
		}
		return -1;
	}

	/**
	 * Add a Mass module to a given subset.
	 *
	 * @param m the mass module to reference.
	 * @param subsetName
	 *            the subset to add the module to.
	 * @return 0 if success, -1 if fail.
	 */
	public int addMassToSubset(Mass m, String subsetName) {
		if (m != null) {
			this.m_mass_subsets.get(subsetName).add(m);
			return 0;
		}
		else
			return -1;
	}


	/**
	 * Create an empty Interaction module subset item. Module references will be associated to
	 * this specific key later.
	 *
	 * @param name
	 *            the identifier for this subset.
	 * @return 0 if success, -1 otherwise.
	 */
	public int createInteractionSubset(String name) {
		if (!this.m_int_subsets.containsKey(name)) {
			this.m_int_subsets.put(name, new ArrayList<>());
			return 0;
		}
		return -1;
	}


	public int addInteractionToSubset(Interaction l, String subsetName) {
		if (l != null) {
			this.m_int_subsets.get(subsetName).add(l);
			return 0;
		}
		return -1;
	}


	private <T extends Module> void setParamForGroup(ArrayList<T> m, param p, double value){
		for(int i = m.size()-1; i >= 0; i--){
			if(m.get(i) == null)
				m.remove(i);
			else
				m.get(i).setParam(p, value);
		}
	}

	public int setParamForMassSubset(String subsetName, param p, double value){
		if (this.m_mass_subsets.containsKey(subsetName)) {
			ArrayList<Mass> mList = this.m_mass_subsets.get(subsetName);
				this.setParamForGroup(mList, p, value);
			return 0;
		}
		else return -1;
	}


	public int setParamForInteractionSubset(String subsetName, param p, double value){
		if (this.m_int_subsets.containsKey(subsetName)) {
			ArrayList<Interaction> iList = this.m_int_subsets.get(subsetName);
			this.setParamForGroup(iList, p, value);
			return 0;
		}
		else return -1;
	}


	public void welcome() {
		System.out.println("##library.name## ##library.prettyVersion## by ##author##");
	}



	/**
	 * return the version of the Library.
	 *
	 * @return String
	 */
	public static String version() {
		return VERSION;
	}

}