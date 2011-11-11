package net.srcdemo;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.decasdev.dokan.CreationDispositionEnum;
import net.decasdev.dokan.DokanFileInfo;
import net.decasdev.dokan.DokanOperationException;
import net.decasdev.dokan.Win32FindData;
import net.srcdemo.audio.AudioHandlerFactory;
import net.srcdemo.video.VideoHandlerFactory;

public class SrcDemoFS extends LoopbackFS
{
	private static final Pattern demoNamePattern = Pattern.compile("\\d+\\.tga$|\\.wav$", Pattern.CASE_INSENSITIVE);
	private static final Win32FindData[] emptyWin32FindData = new Win32FindData[0];
	private final AudioHandlerFactory audioHandlerFactory;
	private final Set<SrcDemoListener> demoListeners = new HashSet<SrcDemoListener>();
	private final ReentrantLock demoLock = new ReentrantLock();
	private final Map<String, SrcDemo> demos = new HashMap<String, SrcDemo>();
	private boolean hideFiles = false;
	private final VideoHandlerFactory videoHandlerFactory;

	public SrcDemoFS(final String backingStorage, final VideoHandlerFactory videoHandlerFactory,
			final AudioHandlerFactory audioHandlerFactory)
	{
		super(backingStorage);
		this.audioHandlerFactory = audioHandlerFactory;
		this.videoHandlerFactory = videoHandlerFactory;
	}

	public void addListener(final SrcDemoListener listener)
	{
		demoListeners.add(listener);
	}

	@Override
	protected void closeFile(final String fileName)
	{
		final SrcDemo demo = getDemo(fileName);
		if (demo == null) {
			super.closeFile(fileName);
		}
		demo.closeFile(fileName);
	}

	@Override
	protected boolean createFile(final String fileName, final CreationDispositionEnum creation)
	{
		if (!creation.shouldCreate()) {
			return super.createFile(fileName, creation);
		}
		final SrcDemo demo = getDemo(fileName);
		if (demo == null) {
			return super.createFile(fileName, creation);
		}
		demo.createFile(fileName);
		return true;
	}

	void destroy(final SrcDemo srcDemo)
	{
		if (srcDemo == null) {
			return;
		}
		String toDelete = null;
		for (final String demoPrefix : demos.keySet()) {
			if (srcDemo.equals(demos.get(demoPrefix))) {
				toDelete = demoPrefix;
				break;
			}
		}
		if (toDelete != null) {
			demoLock.lock();
			demos.remove(toDelete);
			demoLock.unlock();
		}
	}

	@Override
	protected Collection<String> findFiles(final String pathName)
	{
		if (hideFiles) {
			return null;
		}
		else {
			final Collection<String> actualFiles = super.findFiles(pathName);
			demoLock.lock();
			for (final SrcDemo demo : demos.values()) {
				demo.modifyFindResults(pathName, actualFiles);
			}
			demoLock.unlock();
			return actualFiles;
		}
	}

	public void flushAudioBuffer()
	{
		demoLock.lock();
		for (final SrcDemo demo : demos.values()) {
			demo.flushAudioBuffer();
		}
		demoLock.unlock();
	}

	private SrcDemo getDemo(final String fileName)
	{
		final Matcher match = demoNamePattern.matcher(fileName);
		if (!match.find()) {
			return null;
		}
		final String demoName = fileName.substring(0, match.start());
		final String demoNameLowercase = demoName.toLowerCase();
		demoLock.lock();
		if (!demos.containsKey(demoNameLowercase)) {
			demos.put(demoNameLowercase, new SrcDemo(this, demoName, videoHandlerFactory, audioHandlerFactory));
		}
		demoLock.unlock();
		return demos.get(demoNameLowercase);
	}

	@Override
	protected FileInfo getFileInfo(final String fileName)
	{
		final SrcDemo demo = getDemo(fileName);
		if (demo == null) {
			return super.getFileInfo(fileName);
		}
		return demo.getFileInfo(fileName);
	}

	void notifyAudioBuffer(final int occupied, final int total)
	{
		for (final SrcDemoListener listener : demoListeners) {
			listener.onAudioBuffer(occupied, total);
		}
	}

	void notifyAudioBufferWriteout()
	{
		for (final SrcDemoListener listener : demoListeners) {
			listener.onAudioBufferWriteout();
		}
	}

	void notifyFrameProcessed(final String frameName)
	{
		for (final SrcDemoListener listener : demoListeners) {
			listener.onFrameProcessed(frameName);
		}
	}

	void notifyFrameSaved(final File savedFrame)
	{
		for (final SrcDemoListener listener : demoListeners) {
			listener.onFrameSaved(savedFrame);
		}
	}

	@Override
	public Win32FindData[] onFindFiles(final String pathName, final DokanFileInfo fileInfo) throws DokanOperationException
	{
		if (hideFiles) {
			return emptyWin32FindData;
		}
		else {
			return super.onFindFiles(pathName, fileInfo);
		}
	}

	public void removeListener(final SrcDemoListener listener)
	{
		demoListeners.remove(listener);
	}

	public void setHideFiles(final boolean hideFiles)
	{
		this.hideFiles = hideFiles;
	}

	@Override
	protected void truncateFile(final String fileName, final long length)
	{
		final SrcDemo demo = getDemo(fileName);
		if (demo == null) {
			super.truncateFile(fileName, length);
		}
		demo.truncateFile(fileName, length);
	}

	@Override
	protected int writeFile(final String fileName, final ByteBuffer buffer, final long offset)
	{
		final SrcDemo demo = getDemo(fileName);
		if (demo == null) {
			return super.writeFile(fileName, buffer, offset);
		}
		return demo.writeFile(fileName, buffer, offset);
	}
}
