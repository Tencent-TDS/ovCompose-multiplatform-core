/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.appfunctions.schema.notes

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionOpenable
import androidx.appfunctions.AppFunctionSchemaDefinition
import androidx.appfunctions.schema.types.AppFunctionUri

// TODO(b/401517540): Add remaining APIs: UpdateNote, GetNote, folder APIs
/**
 * The category name of Notes related app functions.
 *
 * Example of apps that can support this category of schema include note taking apps.
 *
 * The category is used to search app functions related to notes, using
 * [androidx.appfunctions.AppFunctionSearchSpec.schemaCategory].
 */
public const val APP_FUNCTION_SCHEMA_CATEGORY_NOTES: String = "notes"

/** Finds [AppFunctionNote]s with the given search criteria specified in [Parameters]. */
@AppFunctionSchemaDefinition(
    name = "findNotes",
    version = FindNotesAppFunction.SCHEMA_VERSION,
    category = APP_FUNCTION_SCHEMA_CATEGORY_NOTES
)
public interface FindNotesAppFunction<
    Parameters : FindNotesAppFunction.Parameters,
    Response : FindNotesAppFunction.Response
> {
    /**
     * Finds notes with the given search criteria.
     *
     * @param appFunctionContext The AppFunction execution context.
     * @param parameters The parameters for finding notes.
     * @return The response including the list of notes that match the parameters.
     */
    public suspend fun findNotes(
        appFunctionContext: AppFunctionContext,
        parameters: Parameters,
    ): Response

    /** The parameters for finding notes. */
    // TODO(b/401517540): Add start and end dates when DateTime is supported.
    public interface Parameters {
        /**
         * The search query to be processed. A null value means to query all notes with the
         * remaining parameters.
         *
         * This parameter is analogous to the caller typing a query into a search box. The app
         * providing the app function has full control over how the query is interpreted and
         * processed; for example by name matching or semantic analysis.
         */
        public val query: String?
            get() = null
    }

    /** The response including the list of notes that match the parameters. */
    public interface Response {
        /** The list of notes that match the parameters. */
        public val notes: List<AppFunctionNote>
    }

    public companion object {
        /** Current schema version. */
        @RestrictTo(LIBRARY_GROUP) internal const val SCHEMA_VERSION: Int = 2
    }
}

/** Creates an [AppFunctionNote] with the given parameters. */
@AppFunctionSchemaDefinition(
    name = "createNote",
    version = CreateNoteAppFunction.SCHEMA_VERSION,
    category = APP_FUNCTION_SCHEMA_CATEGORY_NOTES
)
public interface CreateNoteAppFunction<
    Parameters : CreateNoteAppFunction.Parameters,
    Response : CreateNoteAppFunction.Response
> {
    /**
     * Creates an [AppFunctionNote] with the given parameters.
     *
     * @param appFunctionContext The AppFunction execution context.
     * @param parameters The parameters for creating a note.
     * @return The response including the created note.
     */
    public suspend fun createNote(
        appFunctionContext: AppFunctionContext,
        parameters: Parameters,
    ): Response

    /** The parameters for creating a note. */
    public interface Parameters {
        /** The title of the note. */
        public val title: String
        /** The text content of the note. */
        public val content: String?
            get() = null

        /** The attachments of the note. */
        public val attachments: List<AppFunctionNote.Attachment>
            get() = emptyList()

        /**
         * An optional UUID for this note provided by the caller. If provided, the caller can use
         * this UUID as well as the returned [AppFunctionNote.id] to reference this specific note in
         * subsequent requests, such as a request to update the note that was just created.
         *
         * To support [externalUuid], the application should maintain a mapping between the
         * [externalUuid] and the internal id of this note. This allows the application to retrieve
         * the correct note when the caller references it using the provided `externalUuid` in
         * subsequent requests.
         *
         * If the `externalUuid` is not provided by the caller in the creation request, the
         * application should expect subsequent requests from the caller to reference the note using
         * the application generated [AppFunctionNote.id].
         */
        public val externalUuid: String?
            get() = null
    }

    /** The response including the created note. */
    public interface Response {
        /** The created note. */
        public val createdNote: AppFunctionNote
    }

    public companion object {
        /** Current schema version. */
        @RestrictTo(LIBRARY_GROUP) internal const val SCHEMA_VERSION: Int = 2
    }
}

/** Deletes the notes with the given search criteria, defined by {@code parameters}. */
@AppFunctionSchemaDefinition(
    name = "deleteNotes",
    version = DeleteNotesAppFunction.SCHEMA_VERSION,
    category = APP_FUNCTION_SCHEMA_CATEGORY_NOTES
)
public interface DeleteNotesAppFunction<
    Parameters : DeleteNotesAppFunction.Parameters,
    Response : DeleteNotesAppFunction.Response
> {
    /**
     * Deletes the notes with the given search criteria.
     *
     * @param appFunctionContext The AppFunction execution context.
     * @param parameters The parameters defining the criteria of notes to delete.
     * @return A list of successfully deleted note IDs.
     */
    public suspend fun deleteNotes(
        appFunctionContext: AppFunctionContext,
        parameters: Parameters,
    ): Response

    /** The parameters defining the criteria of notes to delete. */
    public interface Parameters {
        /** The IDs of the notes to delete. */
        public val noteIds: List<String>
    }

    /** The response of delete notes request. */
    public interface Response {
        /** A list of successfully deleted note IDs. */
        public val noteIds: List<String>
    }

    public companion object {
        /** Current schema version. */
        @RestrictTo(LIBRARY_GROUP) internal const val SCHEMA_VERSION: Int = 2
    }
}

/**
 * Shows the note with the given parameters.
 *
 * @param Parameters The parameters of the note to show.
 * @param Response The response including the [AppFunctionOpenable] to show the note.
 */
@AppFunctionSchemaDefinition(
    name = "showNote",
    version = ShowNoteAppFunction.SCHEMA_VERSION,
    category = APP_FUNCTION_SCHEMA_CATEGORY_NOTES
)
public interface ShowNoteAppFunction<
    Parameters : ShowNoteAppFunction.Parameters,
    Response : ShowNoteAppFunction.Response
> {
    /**
     * Shows the note with the given parameters.
     *
     * @param appFunctionContext The AppFunction execution context.
     * @param showNoteParams The params of the note to show.
     * @return The response including the intent to show the note.
     */
    public suspend fun showNote(
        appFunctionContext: AppFunctionContext,
        showNoteParams: Parameters
    ): Response

    /** The parameters for [showNote]. */
    public interface Parameters {
        /** The [AppFunctionNote.id] of the note to show. */
        public val noteId: String
    }

    /** The [AppFunctionOpenable] response for [showNote]. */
    public interface Response : AppFunctionOpenable

    public companion object {
        /** Current schema version. */
        @RestrictTo(LIBRARY_GROUP) internal const val SCHEMA_VERSION: Int = 2
    }
}

/** A note entity. */
public interface AppFunctionNote {
    /** The ID of the note. */
    public val id: String

    /** The title of the note. */
    public val title: String

    /** The content of the note. */
    public val content: String?
        get() = null

    /** The attachments of the note. */
    public val attachments: List<Attachment>
        get() = emptyList()

    /** An attached file. */
    public interface Attachment {
        /**
         * The URI of the attached file.
         *
         * When providing an [androidx.appfunctions.schema.types.AppFunctionUri] to another app,
         * that app must be granted URI permission using
         * [android.content.Context.grantUriPermission] to the receiving app.
         *
         * The providing app should also consider revoking the URI permission by using
         * [android.content.Context.revokeUriPermission] after a certain time period.
         */
        public val uri: AppFunctionUri

        /** The display name of the attached file. */
        public val displayName: String

        /** The MIME type of the attached file. Format defined in RFC 6838. */
        public val mimeType: String?
            get() = null
    }
}
