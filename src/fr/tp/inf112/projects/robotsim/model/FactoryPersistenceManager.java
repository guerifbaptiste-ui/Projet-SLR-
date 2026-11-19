package fr.tp.inf112.projects.robotsim.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import fr.tp.inf112.projects.canvas.model.Canvas;
import fr.tp.inf112.projects.canvas.model.CanvasChooser;
import fr.tp.inf112.projects.canvas.model.impl.AbstractCanvasPersistenceManager;

public class FactoryPersistenceManager extends AbstractCanvasPersistenceManager {
	
	public FactoryPersistenceManager(final CanvasChooser canvasChooser) {
		super(canvasChooser);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Canvas read(final String canvasId)
	throws IOException {
		try (
			final InputStream fileInputStream = new FileInputStream(canvasId);
			final InputStream bufferedInputStream = new BufferedInputStream(fileInputStream);
			final ObjectInputStream objectInputStream = new ObjectInputStream(bufferedInputStream);
		) {
			return (Canvas) objectInputStream.readObject();
		}
		catch (final ClassNotFoundException ex) {
			throw new IOException(ex);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void persist(Canvas canvasModel)
	throws IOException {
		try (
			final OutputStream fileOutputStream = new FileOutputStream(canvasModel.getId());
			final OutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
			final ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream);
		) {
			objectOutputStream.writeObject(canvasModel);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean delete(final Canvas canvasModel)
	throws IOException {
		final File canvasFile = new File(canvasModel.getId());
		
		return canvasFile.delete();
	}
}
