package com.pizza;

/**
 * Type of pizza slice
 *
 * @version 1.0
 */
public enum SliceType {
  Veggie,
  Cheese,
  Meat,
  Works;

  /**
   * Tests if pizza slice is vegetarian
   * @return Returns true if pizza slice is vegetarian; false otherwise
   */
  public boolean isVeg() {
    return this == Veggie || this == Cheese;
  }
}
