/**
 * Copyright 2008 - 2019 The Loon Game Engine Authors
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
package loon.component;

import loon.LSystem;
import loon.LTexture;
import loon.LTextures;
import loon.canvas.LColor;
import loon.event.ActionKey;
import loon.event.CallFunction;
import loon.event.SysKey;
import loon.event.SysTouch;
import loon.font.FontSet;
import loon.font.FontUtils;
import loon.font.IFont;
import loon.font.LFont;
import loon.geom.PointF;
import loon.geom.RectF;
import loon.opengl.GLEx;
import loon.utils.MathUtils;
import loon.utils.StringUtils;
import loon.utils.TArray;
import loon.utils.timer.LTimer;

/**
 * 游戏中常见的分行选择型菜单栏,注入几行文字(字符串数组),就会自行产生几行可选菜单
 * 
 * LMenuSelect ms = new LMenuSelect("第一选项,第二个,第三个,第四个,我是第五个", 66, 66); 
 * // 选中行的选择外框渲染颜色,不设置不显示 
 * // ms.setSelectRectColor(LColor.red); 
 * // 选中行所用的图像标记(箭头图之类),不设置使用默认样式 
 * // ms.setImageFlag(LSystem.FRAMEWORK_IMG_NAME+"creese.png"); 
 * // 选择框菜单所用的背景图,不设置使用默认样式,也可以noBackground不显示
 *    ms.setBackground(DefUI.getGameWinFrame(ms.width(), ms.height(),LColor.black,LColor.blue, false)); 
 * // 设置监听 ms.setMenuListener(new
 *    LMenuSelect.ClickEvent() {
 * 
 * // 监听当前点击的索引与内容
 * 
 * @Override public void onSelected(int index, String context) { // 添加气泡提示
 *           
 *           add(LToast.makeText(context, Style.SUCCESS));
 * 
 *           }}); 
 *   // 添加到screen 
 *   add(ms);
 */
public class LMenuSelect extends LComponent implements FontSet<LMenuSelect> {

	public interface ClickEvent {

		public void onSelected(int index, String context);

	}

	private ClickEvent _menuSelectedEvent;

	private ActionKey _touchEvent = new ActionKey(ActionKey.NORMAL);

	private ActionKey _keyEvent = new ActionKey(ActionKey.NORMAL);

	private CallFunction _function;

	private boolean _over, _pressed;

	private int _pressedTime = 0;

	private int _selected = 0;

	private IFont _font;

	private String[] _labels;

	private RectF[] _selectRects;

	private int _selectCountMax = -1;

	private String _flag = LSystem.FLAG_SELECT_TAG;

	private LTexture _flag_image = null;

	private PointF _offsetFont = new PointF();

	private float _flag_text_space;

	private boolean _showRect;

	private boolean _showBackground;

	private float _flagWidth = 0;

	private float _flagHeight = 0;

	private LColor _selectRectColor;

	private LColor _fontColor;

	private LColor _selectedFillColor;

	private LColor _selectBackgroundColor;

	private LTimer _colorUpdate;

	public static LMenuSelect makeMenu(String labels, int x, int y) {
		return new LMenuSelect(labels, x, y);
	}

	public static LMenuSelect makeMenu(String[] labels, int x, int y) {
		return new LMenuSelect(labels, x, y);
	}

	public static LMenuSelect makeMenu(IFont font, String[] labels, int x, int y) {
		return new LMenuSelect(font, labels, x, y);
	}

	public static LMenuSelect makeMenu(IFont font, String[] labels, String path, int x, int y) {
		return new LMenuSelect(font, labels, path, x, y);
	}

	public static LMenuSelect makeMenu(IFont font, String[] labels, LTexture bg, int x, int y) {
		return new LMenuSelect(font, labels, bg, x, y);
	}

	public LMenuSelect(String labels, int x, int y) {
		this(StringUtils.split(labels, ','), x, y);
	}

	public LMenuSelect(String[] labels, int x, int y) {
		this(LFont.getDefaultFont(), labels, x, y);
	}

	public LMenuSelect(IFont font, String[] labels, int x, int y) {
		this(font, labels, (LTexture) null, x, y);
	}

	public LMenuSelect(IFont font, String[] labels, String path, int x, int y) {
		this(font, labels, LTextures.loadTexture(path), x, y);
	}

	public LMenuSelect(IFont font, String[] labels, LTexture bg, int x, int y) {
		this(x, y, 1, 1);
		this._selectRectColor = LColor.white.cpy();
		this._selectedFillColor = LColor.blue.cpy();
		this._selectBackgroundColor = LColor.blue.darker();
		this._fontColor = LColor.white.cpy();
		this._colorUpdate = new LTimer(LSystem.SECOND * 2);
		this._background = bg;
		this._drawBackground = false;
		this._flag_text_space = 10;
		this._showRect = false;
		this._showBackground = true;
		this.setFont(font);
		this.setLabels(labels);
	}

	public LMenuSelect(int x, int y, int width, int height) {
		super(x, y, width, height);
	}

	public LMenuSelect setTextFlag(String flag) {
		if (!_flag.equals(flag)) {
			this._flag = flag;
			this.setLabels(_labels);
		}
		return this;
	}

	public String getTextFlag() {
		return this._flag;
	}

	public LTexture getImageFlag() {
		return this._flag_image;
	}

	public LMenuSelect setImageFlag(LTexture tex) {
		this._flag_image = tex;
		return this;
	}

	public LMenuSelect setImageFlag(String path) {
		return setImageFlag(LTextures.loadTexture(path));
	}

	public LMenuSelect setLabels(String labels) {
		return setLabels(StringUtils.split(labels, ','));
	}

	public LMenuSelect setLabels(String[] labels) {
		_labels = labels;
		if (_labels != null) {
			_selectCountMax = labels.length;
			_selectRects = new RectF[_selectCountMax];
			TArray<CharSequence> chars = new TArray<CharSequence>(_selectCountMax);
			float maxWidth = 0;
			float maxHeight = 0;
			if (_flag_image == null) {
				_flagWidth = _font.stringWidth(_flag);
				_flagHeight = _font.stringHeight(_flag);
			} else {
				_flagWidth = _flag_image.getWidth();
				_flagHeight = _flag_image.getHeight();
			}
			float lastWidth = 0;
			float lastHeight = 0;
			for (int i = 0; i < _labels.length; i++) {
				chars.clear();
				chars = FontUtils.splitLines(_labels[i], chars);
				float height = 0;
				lastWidth = maxWidth;
				lastHeight = maxHeight;
				for (CharSequence ch : chars) {
					maxWidth = MathUtils.max(maxWidth,
							FontUtils.measureText(_font, ch) + _font.getHeight() + _flagWidth + _flag_text_space);
					height += MathUtils.max(_font.stringHeight(new StringBuilder(ch).toString()), _flagHeight);
				}
				if (maxWidth > lastWidth) {
					for (int j = 0; j < _selectRects.length; j++) {
						if (_selectRects[j] != null) {
							_selectRects[j].width = maxWidth;
						}
					}
				}
				if (maxHeight > lastHeight) {
					for (int j = 0; j < _selectRects.length; j++) {
						if (_selectRects[j] != null) {
							_selectRects[j].height = maxHeight;
						}
					}
				}
				height = height + _font.getHeight() / 2;
				_selectRects[i] = new RectF(0, maxHeight, maxWidth, height);
				maxHeight += height;
			}

			setSize(maxWidth + _flag_text_space * 2, maxHeight + _flag_text_space * 2);
		}
		return this;
	}

	public LMenuSelect setMenuListener(ClickEvent event) {
		_menuSelectedEvent = event;
		return this;
	}

	public ClickEvent getMenuListener() {
		return this._menuSelectedEvent;
	}

	@Override
	public LMenuSelect setFont(IFont font) {
		_font = font;
		return this;
	}

	@Override
	public IFont getFont() {
		return _font;
	}

	@Override
	public void createUI(GLEx g, int x, int y, LComponent component, LTexture[] buttonImage) {
		if (!visible) {
			return;
		}
		if (_showBackground && _background != null) {
			if (_selectRects != null && _selectRects.length > 0) {
				RectF rect = _selectRects[0];
				g.draw(_background, x + rect.x - _flag_text_space, y + rect.y - _flag_text_space, getWidth(),
						getHeight());
			}
		} else if (_showBackground) {
			if (_selectRects != null && _selectRects.length > 0) {
				RectF rect = _selectRects[0];
				g.fillRect(x + rect.x - _flag_text_space, y + rect.y - _flag_text_space, getWidth(), getHeight(),
						_selectBackgroundColor.setAlpha(0.5f));
			}
		}
		if (_labels != null) {
			for (int i = 0; i < _labels.length; i++) {
				if (_selectRects != null && i < _selectRects.length) {
					RectF rect = _selectRects[i];
					if (_selected == i) {
						drawSelectedFill(g, _offsetFont.x + x + rect.x, _offsetFont.y + y + rect.y, rect.width,
								rect.height);
					}
					if (_flagWidth == 0 && _flagHeight == 0) {
						g.drawString(_labels[i],
								_offsetFont.x + x + rect.x + (rect.width - _font.stringWidth(_labels[i])) / 2,
								_offsetFont.y + y + rect.y + (rect.height - _font.stringHeight(_labels[i])) / 4,
								_fontColor);
					} else {
						g.drawString(_labels[i],
								_offsetFont.x + x + rect.x + (rect.width - _font.stringWidth(_labels[i])) / 2
										+ _flagWidth,
								_offsetFont.y + y + rect.y + (rect.height - _font.stringHeight(_labels[i])) / 4,
								_fontColor);
						if (_selected == i) {
							if (_flag_image == null) {
								g.drawString(_flag, _offsetFont.x + x + rect.x + _flagWidth / 2,
										_offsetFont.y + y + rect.y + _flagHeight / 8, LColor.yellow);
							} else {
								g.draw(_flag_image, _offsetFont.x + x + rect.x + _flagWidth / 2,
										_offsetFont.y + y + rect.y + _flagHeight / 8);
							}
						}
					}
					if (_showRect) {
						g.drawRect(x + rect.x, y + rect.y, rect.width, rect.height, _selectRectColor);
					}
				}
			}
		}
	}

	protected void drawSelectedFill(GLEx g, float x, float y, float width, float height) {
		int color = g.color();
		g.setColor(_selectedFillColor.getRed(), _selectedFillColor.getGreen(), _selectedFillColor.getBlue(),
				(int) (155 * MathUtils.max(0.5f, _colorUpdate.getPercentage())));
		g.fillRect(x, y, width, height);
		g.drawRect(x, y, width - 2, height - 2);
		g.setColor(color);

	}

	@Override
	public void update(long elapsedTime) {
		if (!visible) {
			return;
		}
		super.update(elapsedTime);
		if (SysTouch.isDown() || SysTouch.isDrag() || SysTouch.isMove()) {
			if (_selectRects != null) {
				for (int i = 0; i < _selectRects.length; i++) {
					RectF touched = _selectRects[i];
					if (touched != null && touched.inside(getUITouchX(), getUITouchY())) {
						_selected = i;
					}
				}
			}
		}

		if (_colorUpdate.action(elapsedTime)) {
			_colorUpdate.refresh();
		}
		if (selected) {
			_pressed = true;
			return;
		}
		if (this._pressedTime > 0 && --this._pressedTime <= 0) {
			this._pressed = false;
		}
	}

	public boolean isTouchOver() {
		return this._over;
	}

	public boolean isTouchPressed() {
		return this._pressed;
	}

	@Override
	protected void processTouchDragged() {
		this._over = this._pressed = this.intersects(this.input.getTouchX(), this.input.getTouchY());
		super.processTouchDragged();
	}

	@Override
	protected void processTouchClicked() {
		this.doClick();
	}

	@Override
	protected void processTouchPressed() {
		if (!visible) {
			return;
		}
		if (!_touchEvent.isPressed()) {
			this._pressed = true;
			this.downClick();
			this._touchEvent.press();
		}
	}

	@Override
	protected void processTouchReleased() {
		if (!visible) {
			return;
		}
		if (_touchEvent.isPressed()) {
			this._pressed = false;
			if (_menuSelectedEvent != null && _labels != null && _labels.length > 0) {
				_menuSelectedEvent.onSelected(_selected, _labels[_selected]);
			}
			this.upClick();
			if (_function != null) {
				_function.call(this);
			}
			_touchEvent.release();
		}
	}

	@Override
	protected void processTouchEntered() {
		this._over = true;
	}

	@Override
	protected void processTouchExited() {
		this._over = this._pressed = false;
	}

	@Override
	protected void processKeyPressed() {
		if (!visible) {
			return;
		}
		if (this.isSelected()) {
			if (!_keyEvent.isPressed()) {
				this._pressedTime = 5;
				this._pressed = true;
				if (input != null) {
					int code = input.getKeyPressed();
					switch (code) {
					case SysKey.UP:
						_selected--;
						_selected = (_selected > 0 ? _selected : 0);
						break;
					case SysKey.DOWN:
						_selected++;
						_selected = (_selected < _labels.length ? _selected : _labels.length - 1);
						break;
					}
				}
				if (_menuSelectedEvent != null && _labels != null && _labels.length > 0) {
					_menuSelectedEvent.onSelected(_selected, _labels[_selected]);
				}
				this.doClick();
				_keyEvent.press();
			}

		}
	}

	@Override
	protected void processKeyReleased() {
		if (!visible) {
			return;
		}
		if (this.isSelected()) {
			if (_keyEvent.isPressed()) {
				this._pressed = false;
				_keyEvent.release();
			}
		}
	}

	public PointF getOffsetFont() {
		return _offsetFont;
	}

	public void setOffsetFont(PointF offset) {
		this._offsetFont = offset;
	}

	public boolean isShowRect() {
		return _showRect;
	}

	public void setShowRect(boolean s) {
		this._showRect = s;
	}

	public LColor getSelectRectColor() {
		return _selectRectColor;
	}

	public void setSelectRectColor(LColor c) {
		this.setShowRect(true);
		this._selectRectColor = c;
	}

	public int getSelected() {
		return _selected;
	}

	public void setSelected(int s) {
		this._selected = s;
	}

	public LColor getSelectedFillColor() {
		return _selectedFillColor;
	}

	public void setSelectedFillColor(LColor s) {
		this._selectedFillColor = s;
	}

	public LTimer getColorUpdateTimer() {
		return _colorUpdate;
	}

	public CallFunction getFunction() {
		return _function;
	}

	public void setFunction(CallFunction f) {
		this._function = f;
	}

	public String[] getLabels() {
		return _labels;
	}

	public LColor getFontColor() {
		return _fontColor;
	}

	public void setFontColor(LColor fc) {
		this._fontColor = fc;
	}

	public LMenuSelect noBackground() {
		this._drawBackground = false;
		this._showBackground = false;
		this._background = null;
		return this;
	}

	@Override
	public LComponent clearBackground() {
		this.noBackground();
		return this;
	}

	public float getFlagTextSpace() {
		return _flag_text_space;
	}

	public LMenuSelect setFlagTextSpace(float f) {
		if (_flag_text_space == f) {
			return this;
		}
		this._flag_text_space = f;
		this.setLabels(_labels);
		return this;
	}

	@Override
	public String getUIName() {
		return "menuselect";
	}

	@Override
	public void close() {
		super.close();
		if (_flag_image != null) {
			_flag_image.close();
		}
	}

}