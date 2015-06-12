/*
 * Copyright (C) 2015 Karumi.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.karumi.expandableselector;

import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import com.karumi.expandableselector.animation.AbstractAnimationListener;
import com.karumi.expandableselector.animation.ResizeAnimation;
import com.karumi.expandableselector.animation.VisibilityAnimatorListener;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * FrameLayout extension used to show a list of ExpandableItems instances which can be collapsed
 * and expanded using an animation.
 */
public class ExpandableSelector extends FrameLayout {

  private static final String Y_ANIMATION = "translationY";
  private static final int NO_RESOURCE_ID = -1;
  private static final int NO_SIZE = -1;
  private static int NO_MARGIN = -1;

  private List<ExpandableItem> expandableItems = Collections.EMPTY_LIST;
  private List<View> buttons = new LinkedList<View>();

  private boolean hideBackgroundIfCollapsed;
  private boolean isCollapsed = true;
  private Drawable expandedBackground;

  public ExpandableSelector(Context context) {
    this(context, null);
  }

  public ExpandableSelector(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ExpandableSelector(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initializeView(attrs);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP)
  public ExpandableSelector(Context context, AttributeSet attrs, int defStyleAttr,
      int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    initializeView(attrs);
  }

  /**
   * Configures a List<ExpandableItem> to be shown. By default, the list of ExpandableItems is
   * going to be shown collapsed. Please take into account that this method creates ImageButtons
   * based on the size of the list passed as parameter. Don't use this library as a RecyclerView
   * and take into account the number of elements to show.
   */
  public void setExpandableItems(List<ExpandableItem> expandableItems) {
    validateExpandableItems(expandableItems);
    this.expandableItems = expandableItems;
    renderExpandableItems();
  }

  public void expand() {
    isCollapsed = false;
    expandContainer();
    int numberOfButtons = buttons.size();
    for (int i = 0; i < numberOfButtons; i++) {
      View button = buttons.get(i);
      button.setVisibility(View.VISIBLE);
      float toY = calculateExpandedYPosition(i);
      ObjectAnimator.ofFloat(button, Y_ANIMATION, toY).start();
    }
    updateBackground();
  }

  public void collapse() {
    isCollapsed = true;
    collapseContainer();
    int numberOfButtons = buttons.size();
    for (int i = 0; i < numberOfButtons; i++) {
      View button = buttons.get(i);
      ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(button, Y_ANIMATION, 0);
      if (i != numberOfButtons - 1) {
        objectAnimator.addListener(new VisibilityAnimatorListener(button, View.INVISIBLE));
      }
      objectAnimator.start();
    }
  }

  //TODO: Replace this with click listeners
  @Override public boolean onTouchEvent(MotionEvent event) {
    if (event.getActionMasked() == MotionEvent.ACTION_UP) {
      if (isCollapsed) {
        expand();
      } else {
        collapse();
      }
    }
    return true;
  }

  private void initializeView(AttributeSet attrs) {
    TypedArray attributes =
        getContext().obtainStyledAttributes(attrs, R.styleable.expandable_selector);
    initializeHideBackgroundIfCollapsed(attributes);
    attributes.recycle();
  }

  private void initializeHideBackgroundIfCollapsed(TypedArray attributes) {
    hideBackgroundIfCollapsed =
        attributes.getBoolean(R.styleable.expandable_selector_hide_background_if_collapsed, false);
    expandedBackground = getBackground();
    updateBackground();
  }

  private void updateBackground() {
    if (!hideBackgroundIfCollapsed) {
      return;
    }
    if (!isCollapsed) {
      setBackgroundDrawable(expandedBackground);
    } else {
      setBackgroundResource(android.R.color.transparent);
    }
  }

  private void renderExpandableItems() {
    int numberOfItems = expandableItems.size();
    for (int i = numberOfItems - 1; i >= 0; i--) {
      View button = initializeButton(i);
      addView(button);
      changeGravityToBottomCenterHorizontal(button);
      configureButton(button, expandableItems.get((i)));
      buttons.add(button);
    }
  }

  private View initializeButton(int expandableItemPosition) {
    ExpandableItem expandableItem = expandableItems.get(expandableItemPosition);
    View button = null;
    Context context = getContext();
    LayoutInflater layoutInflater = LayoutInflater.from(context);
    if (expandableItem.hasDrawableId()) {
      button = layoutInflater.inflate(R.layout.expandable_item_image_button, this, false);
    } else if (expandableItem.hasTitle()) {
      button = layoutInflater.inflate(R.layout.expandable_item_button, this, false);
    }
    int visibility = expandableItemPosition == 0 ? View.VISIBLE : View.INVISIBLE;
    button.setVisibility(visibility);
    return button;
  }

  private void configureButton(View button, ExpandableItem expandableItem) {
    button.setClickable(false);
    if (expandableItem.hasDrawableId()) {
      ImageButton imageButton = (ImageButton) button;
      int drawableId = expandableItem.getDrawableId();
      imageButton.setImageResource(drawableId);
    } else if (expandableItem.hasTitle()) {
      Button textButton = (Button) button;
      String text = expandableItem.getTitle();
      textButton.setText(text);
    }
  }

  private void changeGravityToBottomCenterHorizontal(View view) {
    ((LayoutParams) view.getLayoutParams()).gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
  }

  private float calculateExpandedYPosition(int buttonPosition) {
    int numberOfButtons = buttons.size();
    float y = 0;
    for (int i = numberOfButtons - 1; i > buttonPosition; i--) {
      View button = buttons.get(i);
      y = y + button.getHeight() + getMarginRight(button) + getMarginLeft(button);
    }
    return -y;
  }

  private void expandContainer() {
    float toWidth = getWidth();
    float toHeight = getSumHeight();
    ResizeAnimation resizeAnimation = new ResizeAnimation(this, toWidth, toHeight);
    startAnimation(resizeAnimation);
  }

  private void collapseContainer() {
    float toWidth = getWidth();
    float toHeight = getFirstItemHeight();
    ResizeAnimation resizeAnimation = new ResizeAnimation(this, toWidth, toHeight);
    resizeAnimation.setAnimationListener(new AbstractAnimationListener() {
      @Override public void onAnimationEnd(Animation animation) {
        updateBackground();
      }
    });
    startAnimation(resizeAnimation);
  }

  private int getSumHeight() {
    int sumHeight = 0;
    for (View button : buttons) {
      sumHeight += button.getHeight() + getMarginRight(button) + getMarginLeft(button);
    }
    return sumHeight;
  }

  private int getMarginRight(View view) {
    FrameLayout.LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
    return layoutParams.rightMargin;
  }

  private int getMarginLeft(View view) {
    FrameLayout.LayoutParams layoutParams = (LayoutParams) view.getLayoutParams();
    return layoutParams.leftMargin;
  }

  private float getFirstItemHeight() {
    View firstButton = buttons.get(0);
    int height = firstButton.getHeight();
    FrameLayout.LayoutParams layoutParams = (LayoutParams) firstButton.getLayoutParams();
    int topMargin = layoutParams.topMargin;
    int bottomMargin = layoutParams.bottomMargin;
    return height + topMargin + bottomMargin;
  }

  private void validateExpandableItems(List<ExpandableItem> expandableItems) {
    if (expandableItems == null) {
      throw new IllegalArgumentException(
          "The List<ExpandableItem> passed as argument can't be null");
    }
  }
}
