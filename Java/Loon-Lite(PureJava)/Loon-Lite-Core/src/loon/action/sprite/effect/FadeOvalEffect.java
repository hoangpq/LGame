/**
 * Copyright 2008 - 2015 The Loon Game Engine Authors
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
 * @version 0.5
 */
package loon.action.sprite.effect;

import loon.LSystem;
import loon.action.sprite.Entity;
import loon.canvas.LColor;
import loon.opengl.GLEx;
import loon.utils.MathUtils;
import loon.utils.timer.LTimer;

/**
 * 黑幕过渡效果,画面变成圆形扩散或由圆形向中心集中最终消失
 */
public class FadeOvalEffect extends Entity implements BaseEffect {

	private static final LColor[] OVAL_COLORS = new LColor[5];

	private float max_time;
	private LTimer timer;
	private float elapsed;
	private boolean finished = false;
	private int type = TYPE_FADE_IN;

	public FadeOvalEffect(int type, LColor color) {
		this(type, color, LSystem.viewSize.width, LSystem.viewSize.height);
	}

	public FadeOvalEffect(int type, float w, float h) {
		this(type, LColor.black, 2200, w, h);
	}

	public FadeOvalEffect(int type, LColor oc, float w, float h) {
		this(type, oc, 2200, w, h);
	}

	public FadeOvalEffect(int type, LColor oc, int time, float w, float h) {
		this.type = type;
		this.elapsed = 0;
		this.setSize(w, h);
		this.setColor(oc);
		this.elapsed = 0;
		for (int i = 0; i < OVAL_COLORS.length; i++) {
			OVAL_COLORS[i] = new LColor(oc.r, oc.g, oc.b, 1F - 0.15f * i);
		}
		this.max_time = time;
		this.timer = new LTimer(0);
		this.setRepaint(true);
	}

	public void setDelay(long delay) {
		timer.setDelay(delay);
	}

	public long getDelay() {
		return timer.getDelay();
	}

	@Override
	public boolean isCompleted() {
		return finished;
	}

	@Override
	public void onUpdate(long elapsedTime) {
		if (finished) {
			return;
		}
		if (timer.action(elapsedTime)) {
			if (type == TYPE_FADE_IN) {
				this.elapsed += elapsedTime / 20f;
				float progress = this.elapsed / this.max_time;
				this._width = (_width * MathUtils.pow(1f - progress, 2f));
				this._height = (_height * MathUtils.pow(1f - progress, 2f));
				if (this.elapsed >= this.max_time / 15f) {
					this.elapsed = -1;
					this._width = (this._height = 0f);
					this.finished = true;
				}
			} else {
				this.elapsed += elapsedTime;
				float progress = this.elapsed / this.max_time;
				this._width = (LSystem.viewSize.width * MathUtils.pow(progress, 2f));
				this._height = (LSystem.viewSize.height * MathUtils.pow(progress, 2f));
				if (this.elapsed >= this.max_time) {
					this.elapsed = -1;
					this._width = (this._height = MathUtils.max(LSystem.viewSize.width, LSystem.viewSize.height));
					this.finished = true;
				}
			}
		}
		if (this.finished) {
			if (getSprites() != null) {
				getSprites().remove(this);
			}
		}
	}

	@Override
	public void repaint(GLEx g, float sx, float sy) {
		if (finished) {
			return;
		}
		if (this.elapsed > -1) {

			int old = g.color();
			int size = OVAL_COLORS.length;
			for (int i = size - 1; i >= 0; i--) {
				g.setColor(OVAL_COLORS[i]);
				float w = this._width + i * this._width * 0.1f;
				float h = this._height + i * this._height * 0.1f;
				g.fillOval((g.getWidth() / 2 - w / 2f) + sx + _offset.x, (g.getHeight() / 2 - h / 2f) + sy + _offset.y,
						w, h);
			}
			g.setColor(old);
	
		}
	}

	public int getFadeType() {
		return type;
	}

	@Override
	public void close() {
		super.close();
		this.finished = true;
	}

}
