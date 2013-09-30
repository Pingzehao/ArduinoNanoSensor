package edu.ucr.GLTest;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.os.Bundle;

public class GLTest extends Activity 
{
	private GLSurfaceView mGLView;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        mGLView = new GLSurfaceView(this);
        mGLView.setRenderer(new ClearRenderer());
        //mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        setContentView(mGLView);
    }

    @Override
    protected void onPause() 
    {
        super.onPause();
        mGLView.onPause();
    }

    @Override
    protected void onResume() 
    {	
        super.onResume();
        mGLView.onResume();
    }
}

class ClearRenderer implements GLSurfaceView.Renderer 
{
	private Triangle triangle = new Triangle();
	
    public void onSurfaceCreated(GL10 gl, EGLConfig config) 
    {
        // Do nothing special.
    }

    public void onSurfaceChanged(GL10 gl, int width, int height) 
    {
        if(height == 0) { 						//Prevent A Divide By Zero By
			height = 1; 						//Making Height Equal One
		}

		gl.glViewport(0, 0, width, height); 	//Reset The Current Viewport
		gl.glMatrixMode(GL10.GL_PROJECTION); 	//Select The Projection Matrix
		gl.glLoadIdentity(); 					//Reset The Projection Matrix

		//Calculate The Aspect Ratio Of The Window
		GLU.gluPerspective(gl, 45.0f, (float)width / (float)height, 0.1f, 100.0f);

		gl.glMatrixMode(GL10.GL_MODELVIEW); 	//Select The Modelview Matrix
		gl.glLoadIdentity(); 					//Reset The Modelview Matrix
	}

    public void onDrawFrame(GL10 gl) 
    {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();
        gl.glTranslatef(0.0f, 0.0f, -5.0f);
        triangle.draw(gl);
        
    }
}