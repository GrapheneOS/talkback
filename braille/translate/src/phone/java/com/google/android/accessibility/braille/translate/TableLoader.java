package com.google.android.accessibility.braille.translate;

/** Interface for loading table files. */
public interface TableLoader {
  /** The state of the file. */
  enum FileState {
    FILES_ERROR,
    FILES_NOT_EXTRACTED,
    FILES_EXTRACTED,
  }

  /** Ensures the data files are extracted. */
  void ensureDataFiles();

  /** Returns true if the files are extracted. */
  boolean isExtracted();
}
