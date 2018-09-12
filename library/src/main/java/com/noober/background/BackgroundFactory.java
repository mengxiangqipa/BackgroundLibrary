package com.noober.background;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.v4.util.ArrayMap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;

import com.noober.background.utils.TypeValueHelper;

import org.xmlpull.v1.XmlPullParserException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Map;

import static android.graphics.drawable.GradientDrawable.LINEAR_GRADIENT;

public class BackgroundFactory implements LayoutInflater.Factory {

    private LayoutInflater.Factory mViewCreateFactory;
    private LayoutInflater.Factory2 mViewCreateFactory2;

    private static final Class<?>[] sConstructorSignature = new Class[]{Context.class, AttributeSet.class};
    private final Object[] mConstructorArgs = new Object[2];
    private static final Map<String, Constructor<? extends View>> sConstructorMap = new ArrayMap<>();


    @Override
    public View onCreateView(String name, Context context, AttributeSet attrs) {
        View view = null;
        if (mViewCreateFactory2 != null) {
            view = mViewCreateFactory2.onCreateView(name, context, attrs);
            if (view == null) {
                view = mViewCreateFactory2.onCreateView(null, name, context, attrs);
            }
        } else if (mViewCreateFactory != null) {
            view = mViewCreateFactory.onCreateView(name, context, attrs);
        }
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.background);
        TypedArray pressTa = context.obtainStyledAttributes(attrs, R.styleable.background_press);
        TypedArray selectorTa = context.obtainStyledAttributes(attrs, R.styleable.background_selector);

        try {
            if (typedArray.getIndexCount() == 0 && selectorTa.getIndexCount() == 0 && pressTa.getIndexCount() == 0) {
                return null;
            }
            if (view == null) {
                view = createViewFromTag(context, name, attrs);
            }
            if (view == null) {
                return null;
            }

            GradientDrawable drawable = null;
            StateListDrawable stateListDrawable = null;
            if (selectorTa.getIndexCount() > 0) {
                stateListDrawable = getSelectorDrawable(typedArray, selectorTa);
                view.setClickable(true);
                view.setBackground(stateListDrawable);
            } else if (pressTa.getIndexCount() > 0) {
                drawable = getDrawable(typedArray);
                stateListDrawable = getStateListDrawable(drawable, getDrawable(typedArray), pressTa);
                view.setClickable(true);
                view.setBackground(stateListDrawable);
            } else {
                drawable = getDrawable(typedArray);
                view.setBackground(drawable);
            }

            if (typedArray.getBoolean(R.styleable.background_ripple_enable, false) &&
                    typedArray.hasValue(R.styleable.background_ripple_color)) {
                int color = typedArray.getColor(R.styleable.background_ripple_color, 0);
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Drawable contentDrawable = (stateListDrawable == null ? drawable : stateListDrawable);
                    RippleDrawable rippleDrawable = new RippleDrawable(ColorStateList.valueOf(color), contentDrawable, contentDrawable);
                    view.setClickable(true);
                    view.setBackground(rippleDrawable);
                } else {
                    StateListDrawable tmpDrawable = new StateListDrawable();
                    GradientDrawable unPressDrawable = getDrawable(typedArray);
                    unPressDrawable.setColor(color);
                    tmpDrawable.addState(new int[]{-android.R.attr.state_pressed}, drawable);
                    tmpDrawable.addState(new int[]{android.R.attr.state_pressed}, unPressDrawable);
                    view.setClickable(true);
                    view.setBackground(tmpDrawable);
                }
            }

            return view;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            typedArray.recycle();
            pressTa.recycle();
            selectorTa.recycle();
        }

        return view;
    }

    public void setInterceptFactory(LayoutInflater.Factory factory) {
        mViewCreateFactory = factory;
    }

    public void setInterceptFactory2(LayoutInflater.Factory2 factory) {
        mViewCreateFactory2 = factory;
    }

    private boolean hasSetRadius(float[] radius) {
        boolean hasSet = false;
        for (float f : radius) {
            if (f != 0.0f) {
                hasSet = true;
                break;
            }
        }
        return hasSet;
    }

    private GradientDrawable getDrawable(TypedArray typedArray) throws Exception {
        GradientDrawable drawable = new GradientDrawable();
        float[] cornerRadius = new float[8];
        float sizeWidth = 0;
        float sizeHeight = 0;
        float strokeWidth = -1;
        float strokeDashWidth = 0;
        int strokeColor = 1;
        float strokeGap = 0;
        float centerX = 0;
        float centerY = 0;
        int centerColor = 0;
        int startColor = 0;
        int endColor = 0;
        int gradientType = LINEAR_GRADIENT;
        int gradientAngle = 0;
        Rect padding = new Rect();
        for (int i = 0; i < typedArray.getIndexCount(); i++) {
            int attr = TypeValueHelper.sAppearanceValues.get(typedArray.getIndex(i), -1);
            if (attr == -1) {
                continue;
            }
            int typeIndex = typedArray.getIndex(i);
            if (attr == R.styleable.background_shape) {
                drawable.setShape(typedArray.getInt(typeIndex, 0));
            } else if (attr == R.styleable.background_solid_color) {
                drawable.setColor(typedArray.getColor(typeIndex, 0));
            } else if (attr == R.styleable.background_corners_radius) {
                drawable.setCornerRadius(typedArray.getDimension(typeIndex, 0));
            } else if (attr == R.styleable.background_corners_bottomLeftRadius) {
                cornerRadius[6] = typedArray.getDimension(typeIndex, 0);
                cornerRadius[7] = typedArray.getDimension(typeIndex, 0);
            } else if (attr == R.styleable.background_corners_bottomRightRadius) {
                cornerRadius[4] = typedArray.getDimension(typeIndex, 0);
                cornerRadius[5] = typedArray.getDimension(typeIndex, 0);
            } else if (attr == R.styleable.background_corners_topLeftRadius) {
                cornerRadius[0] = typedArray.getDimension(typeIndex, 0);
                cornerRadius[1] = typedArray.getDimension(typeIndex, 0);
            } else if (attr == R.styleable.background_corners_topRightRadius) {
                cornerRadius[2] = typedArray.getDimension(typeIndex, 0);
                cornerRadius[3] = typedArray.getDimension(typeIndex, 0);
            } else if (attr == R.styleable.background_gradient_angle) {
                gradientAngle = typedArray.getInteger(typeIndex, 0);
            } else if (attr == R.styleable.background_gradient_centerX) {
                centerX = typedArray.getFloat(typeIndex, -1);
            } else if (attr == R.styleable.background_gradient_centerY) {
                centerY = typedArray.getFloat(typeIndex, -1);
            } else if (attr == R.styleable.background_gradient_centerColor) {
                centerColor = typedArray.getColor(typeIndex, 0);
            } else if (attr == R.styleable.background_gradient_endColor) {
                endColor = typedArray.getColor(typeIndex, 0);
            } else if (attr == R.styleable.background_gradient_startColor) {
                startColor = typedArray.getColor(typeIndex, 0);
            } else if (attr == R.styleable.background_gradient_gradientRadius) {
                drawable.setGradientRadius(typedArray.getDimension(typeIndex, 0));
            } else if (attr == R.styleable.background_gradient_type) {
                gradientType = typedArray.getInt(typeIndex, 0);
                drawable.setGradientType(gradientType);
            } else if (attr == R.styleable.background_gradient_useLevel) {
                drawable.setUseLevel(typedArray.getBoolean(typeIndex, false));
            } else if (attr == R.styleable.background_padding_left) {
                padding.left = (int) typedArray.getDimension(typeIndex, 0);
            } else if (attr == R.styleable.background_padding_top) {
                padding.top = (int) typedArray.getDimension(typeIndex, 0);
            } else if (attr == R.styleable.background_padding_right) {
                padding.right = (int) typedArray.getDimension(typeIndex, 0);
            } else if (attr == R.styleable.background_padding_bottom) {
                padding.bottom = (int) typedArray.getDimension(typeIndex, 0);
            } else if (attr == R.styleable.background_size_width) {
                sizeWidth = typedArray.getDimension(typeIndex, 0);
            } else if (attr == R.styleable.background_size_height) {
                sizeHeight = typedArray.getDimension(typeIndex, 0);
            } else if (attr == R.styleable.background_stroke_width) {
                strokeWidth = typedArray.getDimension(typeIndex, 0);
            } else if (attr == R.styleable.background_stroke_color) {
                strokeColor = typedArray.getColor(typeIndex, 0);
            } else if (attr == R.styleable.background_stroke_dashWidth) {
                strokeDashWidth = typedArray.getDimension(typeIndex, 0);
            } else if (attr == R.styleable.background_stroke_dashGap) {
                strokeGap = typedArray.getDimension(typeIndex, 0);
            }
        }
        if (hasSetRadius(cornerRadius)) {
            drawable.setCornerRadii(cornerRadius);
        }
        if (typedArray.hasValue(R.styleable.background_size_width) &&
                typedArray.hasValue(R.styleable.background_size_height)) {
            drawable.setSize((int) sizeWidth, (int) sizeHeight);
        }
        if (typedArray.hasValue(R.styleable.background_stroke_width) &&
                typedArray.hasValue(R.styleable.background_stroke_color)) {
            drawable.setStroke((int) strokeWidth, strokeColor, strokeDashWidth, strokeGap);
        }
        if (typedArray.hasValue(R.styleable.background_gradient_centerX) &&
                typedArray.hasValue(R.styleable.background_gradient_centerY)) {
            drawable.setGradientCenter(centerX, centerY);
        }

        if (typedArray.hasValue(R.styleable.background_gradient_startColor) &&
                typedArray.hasValue(R.styleable.background_gradient_endColor)) {
            int[] colors;
            if (typedArray.hasValue(R.styleable.background_gradient_centerColor)) {
                colors = new int[3];
                colors[0] = startColor;
                colors[1] = centerColor;
                colors[2] = endColor;
            } else {
                colors = new int[2];
                colors[0] = startColor;
                colors[1] = endColor;
            }
            drawable.setColors(colors);
        }
        if (gradientType == LINEAR_GRADIENT &&
                typedArray.hasValue(R.styleable.background_gradient_angle)) {
            gradientAngle %= 360;
            if (gradientAngle % 45 != 0) {
                throw new XmlPullParserException(typedArray.getPositionDescription()
                        + "<gradient> tag requires 'angle' attribute to "
                        + "be a multiple of 45");
            }
            GradientDrawable.Orientation mOrientation = GradientDrawable.Orientation.LEFT_RIGHT;
            switch (gradientAngle) {
                case 0:
                    mOrientation = GradientDrawable.Orientation.LEFT_RIGHT;
                    break;
                case 45:
                    mOrientation = GradientDrawable.Orientation.BL_TR;
                    break;
                case 90:
                    mOrientation = GradientDrawable.Orientation.BOTTOM_TOP;
                    break;
                case 135:
                    mOrientation = GradientDrawable.Orientation.BR_TL;
                    break;
                case 180:
                    mOrientation = GradientDrawable.Orientation.RIGHT_LEFT;
                    break;
                case 225:
                    mOrientation = GradientDrawable.Orientation.TR_BL;
                    break;
                case 270:
                    mOrientation = GradientDrawable.Orientation.TOP_BOTTOM;
                    break;
                case 315:
                    mOrientation = GradientDrawable.Orientation.TL_BR;
                    break;
            }
            drawable.setOrientation(mOrientation);
        }

        if (typedArray.hasValue(R.styleable.background_padding_left) &&
                typedArray.hasValue(R.styleable.background_padding_top) &&
                typedArray.hasValue(R.styleable.background_padding_right) &&
                typedArray.hasValue(R.styleable.background_padding_bottom)) {
            Field paddingField = drawable.getClass().getField("mPadding");
            paddingField.setAccessible(true);
            paddingField.set(drawable, padding);
        }
        return drawable;
    }

    private StateListDrawable getStateListDrawable(GradientDrawable drawable, GradientDrawable pressDrawable, TypedArray typedArray) {
        StateListDrawable stateListDrawable = new StateListDrawable();
        for (int i = 0; i < typedArray.getIndexCount(); i++) {
            int attr = TypeValueHelper.sAppearancePressValues.get(typedArray.getIndex(i), -1);
            if (attr == -1) {
                continue;
            }
            int typeIndex = typedArray.getIndex(i);

            if (attr == R.styleable.background_press_pressed_color) {
                int color = typedArray.getColor(typeIndex, 0);
                pressDrawable.setColor(color);
                stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, pressDrawable);
            } else if (attr == R.styleable.background_press_unpressed_color) {
                int color = typedArray.getColor(typeIndex, 0);
                drawable.setColor(color);
                stateListDrawable.addState(new int[]{-android.R.attr.state_pressed}, drawable);
            }
        }
        return stateListDrawable;
    }

    private StateListDrawable getSelectorDrawable(TypedArray typedArray, TypedArray selectorTa) throws Exception {
        StateListDrawable stateListDrawable = new StateListDrawable();

        for (int i = 0; i < selectorTa.getIndexCount(); i++) {
            int attr = TypeValueHelper.sAppearanceSelectorValues.get(selectorTa.getIndex(i), -1);
            if (attr == -1) {
                continue;
            }
            int typeIndex = selectorTa.getIndex(i);

            if (attr == R.styleable.background_selector_checkable_drawable) {
                setSelectorDrawable(typedArray, selectorTa, stateListDrawable, typeIndex, android.R.attr.state_checkable);
            } else if (attr == R.styleable.background_selector_unCheckable_drawable) {
                setSelectorDrawable(typedArray, selectorTa, stateListDrawable, typeIndex, -android.R.attr.state_checkable);
            } else if (attr == R.styleable.background_selector_pressed_drawable) {
                setSelectorDrawable(typedArray, selectorTa, stateListDrawable, typeIndex, android.R.attr.state_pressed);
            } else if (attr == R.styleable.background_selector_unPressed_drawable) {
                setSelectorDrawable(typedArray, selectorTa, stateListDrawable, typeIndex, -android.R.attr.state_pressed);
            }
        }
        return stateListDrawable;
    }

    private void setSelectorDrawable(TypedArray typedArray, TypedArray selectorTa,
                                     StateListDrawable stateListDrawable, int typeIndex, @AttrRes int functionId) throws Exception {
        int color = 0;
        Drawable resDrawable = null;

        //这里用try catch先判断是否是颜色而不是直接调用getDrawable，为了方便填入的是颜色时可以沿用其他属性,
        //否则如果是其他资源会覆盖app:corners_radius等其他shape属性设置的效果
        try {
            color = selectorTa.getColor(typeIndex, 0);
            if (color == 0) {
                resDrawable = selectorTa.getDrawable(typeIndex);
            }
        } catch (Exception e) {
            resDrawable = selectorTa.getDrawable(typeIndex);
        }
        if (resDrawable == null && color != 0) {
            GradientDrawable tmpDrawable = getDrawable(typedArray);
            tmpDrawable.setColor(color);
            stateListDrawable.addState(new int[]{functionId}, tmpDrawable);
        } else {
            stateListDrawable.addState(new int[]{functionId}, resDrawable);
        }
    }

    private View createViewFromTag(Context context, String name, AttributeSet attrs) {
        if (name.equals("view")) {
            name = attrs.getAttributeValue(null, "class");
        }
        try {
            mConstructorArgs[0] = context;
            mConstructorArgs[1] = attrs;

            if (-1 == name.indexOf('.')) {
                View view = null;
                if ("View".equals(name)) {
                    view = createView(context, name, "android.view.");
                }
                if (view == null) {
                    view = createView(context, name, "android.widget.");
                }
                if (view == null) {
                    view = createView(context, name, "android.webkit.");
                }
                return view;
            } else {
                return createView(context, name, null);
            }
        } catch (Exception e) {
            Log.w("BackgroundLibrary", "cannot create 【" + name + "】 : ");
            return null;
        } finally {
            mConstructorArgs[0] = null;
            mConstructorArgs[1] = null;
        }
    }

    private View createView(Context context, String name, String prefix) throws InflateException {
        Constructor<? extends View> constructor = sConstructorMap.get(name);
        try {
            if (constructor == null) {
                Class<? extends View> clazz = context.getClassLoader().loadClass(
                        prefix != null ? (prefix + name) : name).asSubclass(View.class);

                constructor = clazz.getConstructor(sConstructorSignature);
                sConstructorMap.put(name, constructor);
            }
            constructor.setAccessible(true);
            return constructor.newInstance(mConstructorArgs);
        } catch (Exception e) {
            Log.w("BackgroundLibrary", "cannot create 【" + name + "】 : ");
            return null;
        }
    }
}
