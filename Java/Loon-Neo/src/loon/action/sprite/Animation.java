/**
 * Copyright 2008 - 2011
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * @project loon
 * @author cping
 * @email：javachenpeng@yahoo.com
 * @version 0.1
 */
package loon.action.sprite;

import loon.LRelease;
import loon.LTexture;
import loon.LSystem;
import loon.canvas.LColor;
import loon.opengl.TextureUtils;
import loon.utils.CollectionUtils;
import loon.utils.IArray;
import loon.utils.MathUtils;
import loon.utils.StringUtils;
import loon.utils.TArray;
import loon.utils.res.MovieSpriteSheet;
import loon.utils.timer.LTimer;

/**
 * 动画纹理存储用类
 */
public class Animation implements IArray, LRelease {

	private static class AnimationFrame implements LRelease {

		protected LTexture image;

		protected long endTimer;

		public AnimationFrame(LTexture image, long endTimer) {
			this.image = image;
			this.endTimer = endTimer;
		}

		@Override
		public void close() {
			if (image != null) {
				image.close();
			}
		}
	}

	public static interface AnimationListener {
		public void onComplete(Animation animation);
	}

	protected AnimationListener listener;

	public void setAnimationListener(AnimationListener l) {
		this.listener = l;
	}

	public AnimationListener getAnimationListener() {
		return this.listener;
	}

	protected boolean isRunning, aClosed;

	private TArray<AnimationFrame> frames;

	protected int loopCount, loopPlay;

	protected int currentFrameIndex;

	protected long animTime = 0, totalDuration = 0;

	protected int length;

	protected String animationName;

	private LTimer intervalTime = new LTimer(0);

	public Animation() {
		this(new TArray<AnimationFrame>(CollectionUtils.INITIAL_CAPACITY), 0);
	}

	public Animation(Animation a) {
		this.animationName = a.animationName;
		this.isRunning = a.isRunning;
		this.frames = new TArray<Animation.AnimationFrame>(a.frames);
		this.loopCount = a.loopCount;
		this.loopPlay = a.loopPlay;
		this.currentFrameIndex = a.currentFrameIndex;
		this.animTime = a.animTime;
		this.totalDuration = a.totalDuration;
		this.length = frames.size;
	}

	private Animation(TArray<AnimationFrame> frames, long totalDuration) {
		this.animationName = LSystem.UNKOWN;
		this.loopCount = -1;
		this.frames = frames;
		this.length = frames.size;
		this.totalDuration = totalDuration;
		this.isRunning = true;
		start();
	}

	public static Animation getDefaultAnimation(String fileName) {
		return Animation.getDefaultAnimation(LSystem.loadTexture(fileName));
	}

	public static Animation getDefaultAnimation(final LTexture texture) {
		return Animation.getDefaultAnimation(new LTexture[] { texture }, 1, 65535);
	}

	/**
	 * 转化指定文件为动画图像
	 * 
	 * @param fileName
	 * @param width
	 * @param height
	 * @param timer
	 * @return
	 */
	public static Animation getDefaultAnimation(String fileName, int width, int height, int timer) {
		return Animation.getDefaultAnimation(TextureUtils.getSplitTextures(fileName, width, height), -1, timer);
	}

	/**
	 * 转化指定文件为动画图像
	 * 
	 * @param fileName
	 * @param width
	 * @param height
	 * @param timer
	 * @param filterColor
	 * @return
	 */
	public static Animation getDefaultAnimation(String fileName, int width, int height, int timer, LColor filterColor) {
		return Animation.getDefaultAnimation(
				TextureUtils.getSplitTextures(TextureUtils.filterColor(fileName, filterColor), width, height), -1,
				timer);
	}

	/**
	 * 转化指定文件为动画图像
	 * 
	 * @param fileName
	 * @param maxFrame
	 * @param width
	 * @param height
	 * @param timer
	 * @return
	 */
	public static Animation getDefaultAnimation(String fileName, int maxFrame, int width, int height, int timer) {
		return Animation.getDefaultAnimation(TextureUtils.getSplitTextures(fileName, width, height), maxFrame, timer);
	}

	/**
	 * 转化一组Image为动画图像
	 * 
	 * @param images
	 * @param maxFrame
	 * @param width
	 * @param height
	 * @param timer
	 * @return
	 */
	public static Animation getDefaultAnimation(LTexture[] images, int maxFrame, int timer) {
		if (images == null) {
			return new Animation();
		}
		Animation animation = new Animation();
		if (maxFrame != -1) {
			for (int i = 0; i < maxFrame; i++) {
				animation.addFrame(images[i], timer);
			}
		} else {
			int size = images.length;
			for (int i = 0; i < size; i++) {
				animation.addFrame(images[i], timer);
			}
		}
		return animation;
	}

	/**
	 * 转化一组地址字符串为动画图像
	 * 
	 * @param paths
	 * @param maxFrame
	 * @param timer
	 * @return
	 */
	public static Animation getDefaultAnimation(String[] paths, int maxFrame, int timer) {
		LTexture[] res = new LTexture[paths.length];
		for (int i = 0; i < paths.length; i++) {
			res[i] = LSystem.loadTexture(paths[i]);
		}
		return getDefaultAnimation(res, maxFrame, timer);
	}

	/**
	 * 转化一组地址字符串为动画图像
	 * 
	 * @param paths
	 * @param timer
	 * @return
	 */
	public static Animation getDefaultAnimation(String[] paths, int timer) {
		return getDefaultAnimation(paths, paths.length, timer);
	}

	/**
	 * 转化MovieSpriteSheet为动画资源
	 * 
	 * @param sheet
	 * @param maxFrame
	 * @param timer
	 * @return
	 */
	public static Animation getDefaultAnimation(MovieSpriteSheet sheet, int maxFrame, int timer) {
		return getDefaultAnimation(sheet.getTextures(), maxFrame, timer);
	}

	/**
	 * 克隆一个独立动画
	 */
	public Animation cpy() {
		return new Animation(frames, totalDuration);
	}

	/**
	 * 添加一个动画图像
	 * 
	 * @param image
	 * @param timer
	 */
	public Animation addFrame(LTexture image, long timer) {
		totalDuration += timer;
		frames.add(new AnimationFrame(image, totalDuration));
		length++;
		return this;
	}

	/**
	 * 添加一个动画图像
	 * 
	 * @param fileName
	 * @param timer
	 */
	public Animation addFrame(String fileName, long timer) {
		return addFrame(LSystem.loadTexture(fileName), timer);
	}

	/**
	 * 开始执行动画
	 * 
	 */
	public Animation start() {
		return play(0);
	}

	/**
	 * 开始执行动画
	 * 
	 */
	public Animation play(int idx) {
		animTime = 0;
		if (length > 0) {
			currentFrameIndex = idx;
		}
		this.isRunning = true;
		return this;
	}

	/**
	 * 停止动画播放
	 * 
	 * @return
	 */
	public Animation stop() {
		this.isRunning = false;
		return this;
	}

	/**
	 * 刷新动画为初始状态
	 */
	public Animation reset() {
		animTime = 0;
		currentFrameIndex = 0;
		loopPlay = 0;
		loopCount = -1;
		isRunning = true;
		return this;
	}

	/**
	 * 更新当前动画
	 * 
	 * @param delta
	 */
	public void update(float delta) {
		update((long) (MathUtils.max(delta * 1000, 10)));
	}

	/**
	 * 更新当前动画
	 * 
	 * @param timer
	 */
	public void update(long timer) {
		if (loopCount != -1 && loopPlay > loopCount) {
			return;
		}
		if (totalDuration == 0) {
			return;
		}
		if (isRunning && intervalTime.action(timer)) {
			if (length > 0) {
				animTime += timer;
				if (animTime > totalDuration) {
					if (listener != null) {
						listener.onComplete(this);
					}
					animTime = animTime % totalDuration;
					currentFrameIndex = 0;
					loopPlay++;
				}
				for (; animTime > getFrame(currentFrameIndex).endTimer;) {
					currentFrameIndex++;
				}
			}
		}
	}

	/**
	 * 返回当前动画图象
	 * 
	 * @return
	 */
	public LTexture getSpriteImage() {
		if (length == 0) {
			return null;
		} else {
			final LTexture texture = getFrame(currentFrameIndex).image;
			if (!texture.isLoaded()) {
				texture.loadTexture();
			}
			return texture;
		}
	}

	/**
	 * 返回当前动画图象
	 * 
	 * @param index
	 * @return
	 */
	public LTexture getSpriteImage(int index) {
		if (index < 0 || index >= length) {
			return null;
		} else {
			LTexture texture = getFrame(index).image;
			if (!texture.isLoaded()) {
				texture.loadTexture();
			}
			return texture;
		}
	}

	/**
	 * 返回当前动画当前帧宽度
	 * 
	 * @return
	 */
	public int getWidth() {
		LTexture tex = getSpriteImage();
		if (tex != null) {
			return tex.getWidth();
		}
		return 0;
	}

	/**
	 * 返回当前动画当前帧高度
	 * 
	 * @return
	 */
	public int getHeight() {
		LTexture tex = getSpriteImage();
		if (tex != null) {
			return tex.getHeight();
		}
		return 0;
	}

	/**
	 * 返回当前动画面板
	 * 
	 * @param i
	 * @return
	 */
	private AnimationFrame getFrame(int index) {
		if (index < 0) {
			return frames.get(0);
		} else if (index >= length) {
			return frames.get(length - 1);
		}
		return frames.get(index);
	}

	/**
	 * 设定停止状态
	 * 
	 * @param isStop
	 */
	public void setRunning(boolean runing) {
		this.isRunning = runing;
	}

	/**
	 * 返回动画是否正在播放状态
	 * 
	 */
	public boolean isRunning() {
		return this.isRunning;
	}

	/**
	 * 返回动画是否正在播放状态
	 * 
	 * @return
	 */
	public boolean isPlaying() {
		return isRunning();
	}

	/**
	 * 返回当前动画帧(索引)
	 * 
	 * @return
	 */
	public int getCurrentFrameIndex() {
		return this.currentFrameIndex;
	}

	/**
	 * 设定当前动画帧(索引)
	 * 
	 * @param index
	 * @return
	 */
	public Animation setCurrentFrameIndex(int index) {
		this.currentFrameIndex = MathUtils.clamp(index, 0, MathUtils.min(frames.size, length));
		return this;
	}

	/**
	 * 在当前帧基础上，向前(+)或向后(-)移动指定帧
	 * 
	 * @param n
	 * @return
	 */
	public Animation moveFrame(int n) {
		return this.setCurrentFrameIndex(this.currentFrameIndex + n);
	}

	public int getTotalFrames() {
		return length;
	}

	public int getLoopCount() {
		return loopCount;
	}

	public Animation setLoopCount(int loopCount) {
		this.loopCount = loopCount;
		return this;
	}

	public Animation setDelay(long d) {
		intervalTime.setDelay(d);
		return this;
	}

	public Animation setInterval(long d) {
		return setDelay(d);
	}

	public long getDelay() {
		return intervalTime.getDelay();
	}

	public long getInterval() {
		return getDelay();
	}

	@Override
	public void clear() {
		if (frames != null) {
			for (AnimationFrame frame : frames) {
				if (frame != null) {
					frame.close();
				}
			}
			frames.clear();
		}
		this.length = 0;
	}

	@Override
	public boolean isEmpty() {
		return this.length == 0;
	}

	public boolean isClosed() {
		return aClosed;
	}

	@Override
	public void close() {
		this.clear();
		this.aClosed = true;
	}

	@Override
	public int size() {
		return frames.size;
	}

	public Animation increment() {
		return increment(1);
	}

	public Animation increment(int v) {
		currentFrameIndex += v;
		if (currentFrameIndex >= length) {
			done();
		}
		return this;
	}

	public Animation reduction() {
		return reduction(1);
	}

	public Animation reduction(int v) {
		currentFrameIndex -= v;
		if (currentFrameIndex < 0) {
			done();
		}
		return this;
	}

	public Animation done() {
		if (currentFrameIndex < 0) {
			currentFrameIndex = 0;
		} else {
			currentFrameIndex = length - 1;
		}
		isRunning = false;
		return this;
	}

	public String getAnimationName() {
		return animationName;
	}

	public Animation setAnimationName(String ani) {
		if (StringUtils.isEmpty(ani)) {
			return this;
		}
		this.animationName = ani;
		return this;
	}

}
