package org.helioviewer.jhv.opengl.camera;

import java.util.Date;

import org.helioviewer.jhv.base.physics.Constants;
import org.helioviewer.jhv.base.physics.DifferentialRotation;
import org.helioviewer.jhv.base.wcs.CoordinateSystem;
import org.helioviewer.jhv.base.wcs.HeliocentricCartesianCoordinateSystem;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState;
import org.helioviewer.jhv.opengl.scenegraph.GL3DState.VISUAL_TYPE;
import org.helioviewer.jhv.opengl.scenegraph.math.GL3DMat4d;
import org.helioviewer.jhv.opengl.scenegraph.math.GL3DQuatd;
import org.helioviewer.jhv.opengl.scenegraph.math.GL3DVec3d;
import org.helioviewer.jhv.opengl.scenegraph.rt.GL3DRay;
import org.helioviewer.jhv.viewmodel.changeevent.ChangeEvent;
import org.helioviewer.jhv.viewmodel.changeevent.TimestampChangedReason;
import org.helioviewer.jhv.viewmodel.view.LinkedMovieManager;
import org.helioviewer.jhv.viewmodel.view.TimedMovieView;
import org.helioviewer.jhv.viewmodel.view.View;
import org.helioviewer.jhv.viewmodel.view.ViewListener;
import org.helioviewer.jhv.viewmodel.view.jp2view.datetime.ImmutableDateTime;
import org.helioviewer.jhv.viewmodel.view.opengl.GL3DSceneGraphView;

/**
 * The trackball camera provides a trackball rotation behavior (
 * {@link GL3DTrackballRotationInteraction}) when in rotation mode. It is
 * currently the default camera.
 * 
 * @author Simon Sp�rri (simon.spoerri@fhnw.ch)
 * 
 */
public class GL3DTrackballCamera extends GL3DCamera implements ViewListener {
	public static final double DEFAULT_CAMERA_DISTANCE = 12 * Constants.SunRadius;
	private boolean track;
	private GL3DRay lastMouseRay;
	protected CoordinateSystem viewSpaceCoordinateSystem = new HeliocentricCartesianCoordinateSystem();
	private GL3DTrackballRotationInteraction rotationInteraction;
	private GL3DPanInteraction panInteraction;
	private GL3DZoomBoxInteraction zoomBoxInteraction;

	protected GL3DSceneGraphView sceneGraphView;

	protected GL3DInteraction currentInteraction;

	private Date startDate = null;

	private Date currentDate = null;
	private double currentRotation = 0.0;

	private GL3DVec3d startPosition2D;
	private boolean outside;

	public GL3DTrackballCamera(GL3DSceneGraphView sceneGraphView) {
		this.sceneGraphView = sceneGraphView;
		this.rotationInteraction = new GL3DTrackballRotationInteraction(this,
				sceneGraphView);
		this.panInteraction = new GL3DPanInteraction(this, sceneGraphView);
		this.zoomBoxInteraction = new GL3DZoomBoxInteraction(this,
				sceneGraphView);

		this.currentInteraction = this.rotationInteraction;
	}

	public void activate(GL3DCamera precedingCamera) {
		super.activate(precedingCamera);
		sceneGraphView.addViewListener(this);
	}

	public void viewChanged(View sender, ChangeEvent aEvent) {
		if (track) {
			TimestampChangedReason timestampReason = aEvent
					.getLastChangedReasonByType(TimestampChangedReason.class);
			if ((timestampReason != null)
					&& (timestampReason.getView() instanceof TimedMovieView)
					&& LinkedMovieManager.getActiveInstance().isMaster(
							(TimedMovieView) timestampReason.getView())) {
				if (!LinkedMovieManager.getActiveInstance().isPlaying()) {
					this.resetStartPosition();
				}
				currentDate = timestampReason.getNewDateTime().getTime();

				long timediff = (currentDate.getTime() - startDate
							.getTime()) / 1000;

					double rotation = DifferentialRotation
							.calculateRotationInRadians(0, timediff);

					GL3DQuatd newRotation = GL3DQuatd.createRotation(
							currentRotation - rotation, new GL3DVec3d(0, 1, 0));
					GL3DVec3d newPosition = newRotation.toMatrix().multiply(
							startPosition2D);

					GL3DVec3d tmp = GL3DVec3d.subtract(newPosition,
							startPosition2D);
					this.startPosition2D = newPosition;
					if (GL3DState.get().getState() == VISUAL_TYPE.MODE_3D)
						this.getRotation().rotate(
								GL3DQuatd.createRotation(currentRotation
										- rotation, new GL3DVec3d(0, 1, 0)));

					else if (!outside){
						this.translation.x += tmp.x;
						this.translation.y -= tmp.y;

					}
					// fireCameraMoved();
					this.updateCameraTransformation();
					this.currentRotation = rotation;
					this.startPosition2D = newPosition;
				} else {
					currentRotation = 0.0;
					resetStartPosition();
				}

		}
	}

	private void resetStartPosition() {
		this.startDate = getStartDate();

		double x = this.getTranslation().x;
		double y = this.getTranslation().y;
		if (x*x + y*y < Constants.SunRadius * Constants.SunRadius){
		double z = Math.sqrt(Constants.SunRadius * Constants.SunRadius - x * x
				- y * y);
		this.outside = false;
		this.startPosition2D = new GL3DVec3d(x, y, z);
		this.startPosition2D = this.getRotation().toMatrix()
				.multiply(this.startPosition2D);
		}
		else
			this.outside = true;
	}

	private Date getStartDate() {
		if (LinkedMovieManager.getActiveInstance() == null) {
			return null;
		}
		TimedMovieView masterView = LinkedMovieManager.getActiveInstance()
				.getMasterMovie();
		if (masterView == null) {
			return null;
		}
		ImmutableDateTime idt = masterView.getCurrentFrameDateTime();
		if (idt == null) {
			return null;
		}
		return idt.getTime();
	}

	public void applyCamera(GL3DState state) {
		// ((HEEQCoordinateSystem)this.viewSpaceCoordinateSystem).setObservationDate(state.getCurrentObservationDate());
		super.applyCamera(state);
	}

	public void setSceneGraphView(GL3DSceneGraphView sceneGraphView) {
		this.sceneGraphView = sceneGraphView;
	}

	public void reset() {
		this.currentInteraction.reset(this);
	}

	public double getDistanceToSunSurface() {
		return -this.getCameraTransformation().translation().z;
	}

	public GL3DInteraction getPanInteraction() {
		return this.panInteraction;
	}

	public GL3DInteraction getRotateInteraction() {
		return this.rotationInteraction;
	}

	public GL3DInteraction getCurrentInteraction() {
		return this.currentInteraction;
	}

	public void setCurrentInteraction(GL3DInteraction currentInteraction) {
		this.currentInteraction = currentInteraction;
	}

	public GL3DInteraction getZoomInteraction() {
		return this.zoomBoxInteraction;
	}

	public GL3DRay getLastMouseRay() {
		return lastMouseRay;
	}

	public CoordinateSystem getViewSpaceCoordinateSystem() {
		return this.viewSpaceCoordinateSystem;
	}

	public GL3DMat4d getVM() {
		GL3DMat4d c = this.getCameraTransformation().copy();
		return c;
	}

	public String getName() {
		return "Trackball";
	}

	@Override
	public void setTrack(boolean track) {
		this.track = track;
	}
}