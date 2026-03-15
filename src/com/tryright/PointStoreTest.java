package com.tryright;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("PointStore")
class PointStoreTest {
  private static File binTestFile;
  private static File binBadFile;
  private static File textTestFile;
  private static File textBadFile;

  @BeforeAll
  public static void setUp() throws IOException {
    binTestFile = File.createTempFile("binTestFile", "dat");
    try (OutputStream out = new FileOutputStream(binTestFile)) {
      out.write(new byte[] {0,0,0,3,0,0,0,4});
      out.write(new byte[] {0,0,0,0,0,0,0,0});
      out.write(new byte[] {0,0,0,3,0,0,0,6});
      out.write(new byte[] {0,0,0,7,0,0,0,4});
      out.write(new byte[] {0,0,0,3,0,0,0,11});
    }
    binBadFile = File.createTempFile("binBadFile", "dat");
    try (OutputStream out = new FileOutputStream(binBadFile)) {
      out.write(new byte[] {0,0,0,3,0,0,0,4});
      out.write(new byte[] {0,0,0,0,0,0,0});
    }
    textTestFile = File.createTempFile("textTestFile", "txt");
    try (PrintWriter out = new PrintWriter(textTestFile)) {
      out.println("5");
      out.println("3 4");
      out.println("0 0");
      out.println("3 6");
      out.println("7 4");
      out.println("3 11");
    }
    textBadFile = File.createTempFile("textBadFile", "txt");
    try (PrintWriter out = new PrintWriter(textBadFile)) {
      out.println("5");
      out.println("3 4");
      out.println("0 0");
      out.println("3 6");
      out.println("7 4");
    }
  }

  @AfterAll
  public static void tearDown() throws IOException {
    binTestFile.delete();
    binBadFile.delete();
    textTestFile.delete();
    textBadFile.delete();
  }

  static Stream<PointStore> testFiles() throws IOException {
      return Stream.of(new TextPointStore(textTestFile.getAbsolutePath()),
          new BinPointStore(binTestFile.getAbsolutePath()));
  }

  @ParameterizedTest
  @DisplayName("correct format")
  @MethodSource
  void testFiles(PointStore store) throws IOException {
    assertEquals(3, store.getX(0));
    assertEquals(4, store.getY(0));
    assertEquals(0, store.getX(1));
    assertEquals(0, store.getY(1));
    assertEquals(3, store.getX(2));
    assertEquals(6, store.getY(2));
    assertEquals(7, store.getX(3));
    assertEquals(4, store.getY(3));
    assertEquals(3, store.getX(4));
    assertEquals(11, store.getY(4));
    assertEquals(5, store.numPoints());
    assertThrows(IndexOutOfBoundsException.class, () -> store.getX(5));
    assertThrows(IndexOutOfBoundsException.class, () -> store.getY(5));
    assertThrows(IndexOutOfBoundsException.class, () -> store.getX(-1));
    assertThrows(IndexOutOfBoundsException.class, () -> store.getY(-1));
  }

  static Stream<ThrowingSupplier<PointStore>> testBadFiles() throws IOException {
    return Stream.of(
        () -> new TextPointStore(textBadFile.getAbsolutePath()),
        () -> new BinPointStore(binBadFile.getAbsolutePath())
    );
  }

  @ParameterizedTest
  @DisplayName("incorrect format")
  @MethodSource
  void testBadFiles(ThrowingSupplier<PointStore> constructor) throws IOException {
    assertThrows(IOException.class, constructor::get);
  }
}
