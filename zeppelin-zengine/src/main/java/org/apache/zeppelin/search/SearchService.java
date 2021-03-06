/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.zeppelin.search;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteEventAsyncListener;
import org.apache.zeppelin.notebook.Paragraph;

/**
 * Search (both, indexing and query) the notes.
 * 
 * Intended to have multiple implementation, i.e:
 *  - local Lucene (in-memory, on-disk)
 *  - remote Elasticsearch
 */
public abstract class SearchService extends NoteEventAsyncListener {

  public SearchService(String name) {
    super(name);
  }

  /**
   * Full-text search in all the notes
   *
   * @param queryStr a query
   * @return A list of matching paragraphs (id, text, snippet w/ highlight)
   */
  public abstract List<Map<String, String>> query(String queryStr);

  /**
   * Updates all documents in index for the given note:
   *  - name
   *  - all paragraphs
   *
   * @param note a Note to update index for
   * @throws IOException
   */
  public abstract void updateIndexDoc(Note note) throws IOException;

  /**
   * Indexes full collection of notes: all the paragraphs + Note names
   *
   * @param collection of Notes
   */
  public abstract void addIndexDocs(Collection<Note> collection);

  /**
   * Indexes the given note.
   *
   * @throws IOException If there is a low-level I/O error
   */
  public abstract void addIndexDoc(Note note);

  /**
   * Deletes all docs on given Note from index
   */
  public abstract void deleteIndexDocs(String noteId);

  /**
   * Deletes doc for a given
   *
   * @param noteId
   * @param p
   * @throws IOException
   */
  public abstract void deleteIndexDoc(String noteId, Paragraph p);

  /**
   * Frees the recourses used by index
   */
  public void close() {
    super.close();
  }

  @Override
  public void handleNoteCreateEvent(NoteCreateEvent noteCreateEvent) {
    addIndexDoc(noteCreateEvent.getNote());
  }

  @Override
  public void handleNoteRemoveEvent(NoteRemoveEvent noteRemoveEvent) {
    deleteIndexDocs(noteRemoveEvent.getNote().getId());
  }

  @Override
  public void handleNoteUpdateEvent(NoteUpdateEvent noteUpdateEvent) {
    try {
      updateIndexDoc(noteUpdateEvent.getNote());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void handleParagraphCreateEvent(ParagraphCreateEvent paragraphCreateEvent) {
    try {
      updateIndexDoc(paragraphCreateEvent.getParagraph().getNote());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void handleParagraphRemoveEvent(ParagraphRemoveEvent paragraphRemoveEvent) {
    Paragraph p = paragraphRemoveEvent.getParagraph();
    deleteIndexDoc(p.getNote().getId(), p);
  }

  @Override
  public void handleParagraphUpdateEvent(ParagraphUpdateEvent paragraphUpdateEvent) {
    try {
      updateIndexDoc(paragraphUpdateEvent.getParagraph().getNote());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
