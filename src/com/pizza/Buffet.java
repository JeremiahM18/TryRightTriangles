/****************************************
 *
 * Author: Jeremiah McDonald
 * Assignment: Program 4
 * Class: CSC-4180 Operating Systems
 *
 ***************************************/

package com.pizza;

import java.util.List;

/**
 * Class to manage pizza buffet
 * 
 * For all methods, you should handle interrupts internally. Do not let an
 * interrupt terminate a method or cause it to behave incorrectly.
 * 
 * @version 1.0
 */
public interface Buffet {
  /**
   * Called by non-vegetarian patrons to request a desired number of slices and
   * return a list of slices, oldest slice first. This method will block callers
   * until they can have all the desired slices; it won't take any slices from
   * the buffet until they can get everything they want. If a vegetarian is
   * waiting for slices, this will only take non-vegetarian slices (slices other
   * than cheese and veggie). Otherwise, this method will give them any type of
   * slice. If the buffet is closed, this method will return null.
   * 
   * @param desired
   *          number of desired slices
   * 
   * @throws IllegalArgumentException
   *           if desired < 0 or desired > maxSlices
   * @return list of slices or null if buffet closed
   */
  List<SliceType> TakeAny(int desired);

  /**
   * This method is like takeAny(), but it returns the oldest vegetarian slices
   * in the slice list. As with takeAny(), it will block the caller until they
   * can have all the slices they want. Once the buffet is closed, this method
   * will return null.
   * 
   * @param desired
   *          number of desired slices
   * 
   * @throws IllegalArgumentException
   *           if desired < 0 or desired > maxSlices
   * @return list of slices or null if buffet closed
   */
  List<SliceType> TakeVeg(int desired);

  /**
   * This method adds count slices, all of the given slice type, stype, to the
   * buffet. The buffet only has a finite capacity of slices (given to
   * constructor). If there isn't enough room for all the slices, it will add as
   * many as it can to the buffet and then block the calling thread until
   * there's room for more. (Note that this is different from the behavior of
   * takeAny() an takeVeg(), where you don't take any slices until you can have
   * all of them.) This method immediately returns false when the buffet is
   * closed.
   * 
   * @param count
   *          number of slices to add to buffet
   * @param stype
   *          type of slices to add to buffet
   * 
   * @throws IllegalArgumentException
   *           if count < 0
   * @throws NullPointerException
   *           if stype is null
   * @return true if slices were added or false if buffet closed
   */
  boolean AddPizza(int count, SliceType stype);

  /**
   * This function marks the buffet as closed, causing threads blocked in
   * takeAny(), takeVeg() and addPizza() to return with false/null.  Threads
   * subsequently calling these function also immediately return with false/null.
   */
  void close();
}
