/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.qubership.itool.tasks.confluence;

import org.qubership.itool.tasks.AbstractAggregationTaskVerticle;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.pointer.JsonPointer;

import org.qubership.itool.modules.confluence.ConfluenceClient;
import org.qubership.itool.modules.graph.Graph;
import org.qubership.itool.utils.ConfigUtils;
import org.qubership.itool.utils.FSUtils;
import org.qubership.itool.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.qubership.itool.utils.ConfigProperties.CONFLUENCE_SPACE_POINTER;
import static org.qubership.itool.utils.ConfigProperties.RELEASE_POINTER;
import static org.qubership.itool.utils.ConfigProperties.UPLOAD_CONFLUENCE_PAGES_POINTER;
import static org.qubership.itool.utils.ConfigProperties.UPLOAD_KEY_ALL;
import static org.qubership.itool.utils.ConfigProperties.UPLOAD_KEY_NONE;


public class ConfluenceUploadPagesVerticle extends AbstractAggregationTaskVerticle {
    public static final String TRASH_PAGE_NAME = "TRASH";

    public static final String STRUCT_TYPE = "type";
    public static final String STRUCT_CHILDREN = "children";

    public static final String PAGE_TITLE = "title";
    public static final String PAGE_PARENT_TITLE = "parentTitle";
    public static final String PAGE_ON_DISK_PATH = "onDiskPath";
    public static final String PAGE_PARENT_ID = "parentId";
    public static final String PAGE_ID = "id";
    public static final String PAGE_TYPE = "type";

    public static final String ACTION_DELETE = "delete";
    public static final String ACTION_CREATE = "create";
    public static final String ACTION_UPDATE = "update";
    public static final String ACTION_MOVE   = "move";
    public static final String ACTION_KEY_GENERATED_PAGE = "generatedPage";
    public static final String ACTION_KEY_CONFLUENCE_PAGE = "confluencePage";
    public static final String ACTION_KEY_ACTION = "action";
    public static final String ACTION_KEY_CONFLUENCE_PARENT = "confluenceParent";

    public static final JsonPointer ACTION_POINTER_CONFLUENCE_PAGE_TITLE =
            JsonPointer.create().append(ACTION_KEY_CONFLUENCE_PAGE).append(PAGE_TITLE);
    public static final JsonPointer ACTION_POINTER_GENERATED_PAGE_TITLE =
            JsonPointer.create().append(ACTION_KEY_GENERATED_PAGE).append(PAGE_TITLE);
    public static final JsonPointer ACTION_POINTER_GENERATED_PAGE_TYPE =
            JsonPointer.create().append(ACTION_KEY_GENERATED_PAGE).append(PAGE_TYPE);
    public static final JsonPointer ACTION_POINTER_CONFLUENCE_PARENT_ID =
            JsonPointer.create().append(ACTION_KEY_CONFLUENCE_PARENT).append(PAGE_ID);
    public static final JsonPointer ACTION_POINTER_GENERATED_PAGE_PARENT_TITLE =
            JsonPointer.create().append(ACTION_KEY_GENERATED_PAGE).append(PAGE_PARENT_TITLE);

    public static final String PAGE_KEY_STRUCTURE = "structure";
    public static final String PAGE_KEY_CONFLUENCE = "confluence";
    public static final String PAGE_KEY_GENERATED_PAGE = "generatedPage";

    public static final JsonPointer PAGE_POINTER_GENERATED_PAGE =
            JsonPointer.create().append(PAGE_KEY_GENERATED_PAGE);
    public static final JsonPointer PAGE_POINTER_CONFLUENCE =
            JsonPointer.create().append(PAGE_KEY_CONFLUENCE);
    public static final JsonPointer PAGE_POINTER_STRUCTURE_CHILDREN =
            JsonPointer.create().append(PAGE_KEY_STRUCTURE).append(STRUCT_CHILDREN);
    public static final JsonPointer PAGE_POINTER_GENERATED_PAGE_TITLE =
            JsonPointer.create().append(PAGE_POINTER_GENERATED_PAGE).append(PAGE_TITLE);
    public static final JsonPointer PAGE_POINTER_CONFLUENCE_TITLE =
            JsonPointer.create().append(PAGE_POINTER_CONFLUENCE).append(PAGE_TITLE);
    public static final JsonPointer PAGE_POINTER_CONFLUENCE_CHILDREN =
            JsonPointer.create().append(PAGE_POINTER_CONFLUENCE).append(STRUCT_CHILDREN);
    public static final JsonPointer PAGE_POINTER_GENERATED_PAGE_ON_DISK_PATH =
            JsonPointer.create().append(PAGE_POINTER_GENERATED_PAGE).append(PAGE_ON_DISK_PATH);

    protected Logger LOGGER = LoggerFactory.getLogger(ConfluenceUploadPagesVerticle.class);
    private String release = null;
    private String uploadConfluencePages = null;

    @Resource
    @Nullable
    private ConfluenceClient confluenceClient;

    @Override
    protected String[] features() {
        return new String[] { "confluenceUpload" };
    }

    @Override
    protected void taskStart(Promise<?> taskPromise) {
        release = ConfigUtils.getConfigValue(RELEASE_POINTER, config());
        uploadConfluencePages = ConfigUtils.getConfigValue(UPLOAD_CONFLUENCE_PAGES_POINTER, config());

        if (confluenceClient == null) {
            getLogger().warn("Offline mode. Exitting.");
            taskCompleted(taskPromise);
            return;
        }

        if (!uploadRequired(uploadConfluencePages)){
            taskCompleted(taskPromise);
            return;
        }

        JsonArray generatedConfluencePages = this.graph.getVertex(Graph.V_ROOT).getJsonArray("confluencePages");
        JsonObject confluenceStructure = null;
        try {
            confluenceStructure = JsonUtils.readJsonFile(FSUtils.getConfigFilePath(config(), "config", "confluenceStructure.json"));
        } catch (IOException /* | DecodeException */ e) {
            report.exceptionThrown(new JsonObject().put(PAGE_ID, "iTool"), e);
        }

        JsonObject finalConfluenceStructure = confluenceStructure;
        String spaceKey = ConfigUtils.getConfigValue(CONFLUENCE_SPACE_POINTER, config());
        Future trashFuture = confluenceClient.getConfluencePageInfo(spaceKey, TRASH_PAGE_NAME);
        Future existingTreeFuture = buildExistingConfluenceTree(spaceKey, finalConfluenceStructure);
        CompositeFuture.join(trashFuture, existingTreeFuture)
                .onComplete(res -> {
                    JsonObject trashPage = (JsonObject) trashFuture.result();
                    JsonObject realConfluenceTree = (JsonObject) existingTreeFuture.result();

                    JsonArray actionsRequired = buildActions(realConfluenceTree, finalConfluenceStructure, generatedConfluencePages);

                    logActions(actionsRequired);

                    // merge actions for deletion and creation of pages into update action
                    JsonArray mergedActions = mergeActions(actionsRequired);

                    logActions(mergedActions);
                    List<Future> actionFutures = new ArrayList<>();
                    getLogger().info("Property {} is set to {}", UPLOAD_CONFLUENCE_PAGES_POINTER, uploadConfluencePages);
                    // Perform all the "create" actions first
                    List<JsonObject> createActions = mergedActions.stream().map(action -> (JsonObject) action)
                            .filter(action -> ACTION_CREATE.equals(action.getString(ACTION_KEY_ACTION)))
                            .collect(Collectors.toList());
                    createPagesFromActions(createActions, spaceKey, trashPage)
                            .onComplete(result -> {
                                // Perform all the remaining actions
                                mergedActions.stream().map(action -> (JsonObject) action)
                                        .filter(action -> !ACTION_CREATE.equals(action.getString(ACTION_KEY_ACTION)))
                                        .filter(action -> considerToPerformAction(uploadConfluencePages, action))
                                        .forEach(action ->
                                                actionFutures.add(performAction(spaceKey, trashPage, action))
                                        );
                                completeCompositeTask(actionFutures, taskPromise);
                            });
                });
    }

    private Future createPagesFromActions(List<JsonObject> createActions, String spaceKey, JsonObject trashPage) {
        Queue<JsonObject> queue = new LinkedList<>();
        queue.addAll(createActions);
        List<JsonObject> parentedActions = new LinkedList<>();
        List<JsonObject> orphanActions = new LinkedList<>();
        while (!queue.isEmpty()) {
            JsonObject nextAction = queue.remove();
            if (null != nextAction.getString(PAGE_PARENT_ID)) {
                parentedActions.add(nextAction);
            } else {
                orphanActions.add(nextAction);
            }
        }
        List<Future> actionFutures = new ArrayList<>();
        parentedActions.stream()
                .filter(action -> considerToPerformAction(uploadConfluencePages, action))
                .forEach(action ->
                        actionFutures.add(performAction(spaceKey, trashPage, action))
                );
        return CompositeFuture.join(actionFutures).compose(res -> {
            orphanActions.stream().forEach(orphanAction -> {
                actionFutures.stream()
                        .map(result -> (JsonObject) result.result())
                        .forEach(result -> {
                            String newPageId = result.getString(PAGE_ID);
                            String orphanActionParentTitle = (String) ACTION_POINTER_GENERATED_PAGE_PARENT_TITLE.queryJson(orphanAction);
                            String newPageTitle = result.getString(PAGE_TITLE);
                            if (newPageTitle.equals(buildPageTitle(orphanActionParentTitle))){
                                orphanAction.put(PAGE_PARENT_ID, newPageId);
                                getLogger().debug("'{}': New parent id set to {}",
                                        ACTION_POINTER_GENERATED_PAGE_TITLE.queryJson(orphanAction), newPageId);
                            }
                        });
                if (orphanAction.getString(PAGE_PARENT_ID) == null) {
                    getLogger().debug("'{}': Still can not find parent ID", ACTION_POINTER_GENERATED_PAGE_TITLE.queryJson(orphanAction));
                }
            });
            if(orphanActions.isEmpty()) {
                return Future.succeededFuture();
            }
            return createPagesFromActions(orphanActions, spaceKey, trashPage);
        });
    }

    private void logActions(JsonArray actions){
        if (getLogger().isDebugEnabled()) {
            String actionsString = actions.stream().map(actionObj -> {
                JsonObject action = (JsonObject) actionObj;
                return action.getString(ACTION_KEY_ACTION) + ":'" + action.getString(PAGE_TITLE) + "' (parentId=" + action.getString(PAGE_PARENT_ID) + ")";
            }).collect(Collectors.joining("; \n"));
            getLogger().debug("Actions list: \n{}", actionsString);
        }
    }

    private JsonArray mergeActions(JsonArray actionsNeeded) {
        Map<String, List<JsonObject>> groupedActions = actionsNeeded.stream()
                .map(action -> ((JsonObject) action))
                .collect(Collectors.groupingBy(action -> action.getString(ACTION_KEY_ACTION)));
        if (!groupedActions.containsKey(ACTION_DELETE) || !groupedActions.containsKey(ACTION_CREATE)) {
            getLogger().debug("No changes required to confluence actions. Actions planned: {}", actionsNeeded.size());
            return actionsNeeded;
        }

        getLogger().debug("Planned actions before merge: {}", actionsNeeded.size());

        Collection<JsonObject> createActions = getActionsByType(groupedActions, ACTION_CREATE);

        Collection<JsonObject> deleteActions = getActionsByType(groupedActions, ACTION_DELETE);

        Iterator<JsonObject> createActionsIter = createActions.iterator();
        JsonArray mergedActions = new JsonArray();

        while (createActionsIter.hasNext()) {
            JsonObject createAction = createActionsIter.next();
            Iterator<JsonObject> deleteActionsIter = deleteActions.iterator();

            while (deleteActionsIter.hasNext()) {
                JsonObject deleteAction = deleteActionsIter.next();
                if (createAction.getString(PAGE_TITLE).equals(deleteAction.getString(PAGE_TITLE))) {
                    JsonObject mergedAction = new JsonObject()
                            .put(PAGE_TITLE, createAction.getString(PAGE_TITLE))
                            .put(ACTION_KEY_CONFLUENCE_PAGE, deleteAction.getJsonObject(ACTION_KEY_CONFLUENCE_PAGE))
                            .put(ACTION_KEY_GENERATED_PAGE, createAction.getJsonObject(ACTION_KEY_GENERATED_PAGE))
                            .put(PAGE_PARENT_ID, createAction.getString(PAGE_PARENT_ID));
                    // In case of created structure pages without generated content we need to move the page instead of updating it
                    if (null != ACTION_POINTER_GENERATED_PAGE_TYPE.queryJson(createAction)) {
                        mergedAction.put(ACTION_KEY_ACTION, ACTION_UPDATE);
                    } else {
                        mergedAction.put(ACTION_KEY_ACTION, ACTION_MOVE);
                    }
                    createActionsIter.remove();
                    deleteActionsIter.remove();
                    getLogger().debug("'{}': Actions merged into '{}'", mergedAction.getString(PAGE_TITLE), mergedAction.getString(ACTION_KEY_ACTION));
                    mergedActions.add(mergedAction);
                }
            }
        }
        JsonArray resultingActions = new JsonArray(new ArrayList(createActions));

        resultingActions.addAll(new JsonArray(new ArrayList(deleteActions)));
        // same as "update"
        resultingActions.addAll(mergedActions);
        if (groupedActions.containsKey(ACTION_UPDATE)) {
            resultingActions.addAll(new JsonArray(groupedActions.get(ACTION_UPDATE)));
        }
        if (groupedActions.containsKey(ACTION_MOVE)) {
            resultingActions.addAll(new JsonArray(groupedActions.get(ACTION_MOVE)));
        }

        getLogger().debug("Planned actions after merge: {}", resultingActions.size());
        return resultingActions;
    }

    private Collection<JsonObject> getActionsByType(Map<String, List<JsonObject>> groupedActions, String actionName) {
        List<JsonObject> duplicatedActions = new ArrayList<>();
        Collection<JsonObject> result = groupedActions.get(actionName).stream()
                .collect(Collectors.<JsonObject, String, JsonObject>toMap(action ->
                        action.getString(PAGE_TITLE), action -> action, (firstAction, secondAction) -> {
                    duplicatedActions.add(secondAction);
                    return firstAction;
                }))
                .values();
        if (duplicatedActions.size() > 0) {
            report.internalError("Found duplicated " + actionName + " actions for pages with title: "
                    + duplicatedActions.stream().map(action -> action.getString(PAGE_TITLE))
                            .distinct()
                            .collect(Collectors.joining(", ")));
        }
        return result;
    }

    private String getPageTitleFromAction(JsonObject action) {
        String pageTitle = buildPageTitle((String) ACTION_POINTER_GENERATED_PAGE_TITLE.queryJson(action));
        if (pageTitle == null) {
            pageTitle = (String) ACTION_POINTER_CONFLUENCE_PAGE_TITLE.queryJson(action);
        }
        return pageTitle;
    }

    private Future<JsonObject> performAction(String spaceKey, JsonObject trashPage, JsonObject action) {
        JsonObject generatedPage = action.getJsonObject(ACTION_KEY_GENERATED_PAGE);
        switch (action.getString(ACTION_KEY_ACTION)) {
            case ACTION_CREATE:
                if (generatedPage.containsKey(PAGE_ON_DISK_PATH)){
                    // Upload new page (move page to new parent if parentId is present)
                    return confluenceClient.updateConfluencePage(spaceKey,
                            generatedPage.getString(PAGE_TITLE),
                            generatedPage.getString(PAGE_PARENT_TITLE),
                            generatedPage.getString(PAGE_ON_DISK_PATH),
                            release);
                } else {
                    // Only create the page
                    return confluenceClient.createOrMoveConfluencePage(spaceKey,
                            generatedPage.getString(PAGE_TITLE),
                            action.getString(PAGE_PARENT_ID),
                            release);
                }
            case ACTION_UPDATE:
                return confluenceClient.updateConfluencePage(spaceKey,
                        generatedPage.getString(PAGE_TITLE),
                        generatedPage.getString(PAGE_PARENT_TITLE),
                        generatedPage.getString(PAGE_ON_DISK_PATH),
                        release);
            case ACTION_DELETE:
                return confluenceClient.moveConfluencePage(action.getJsonObject(ACTION_KEY_CONFLUENCE_PAGE),
                        trashPage.getString(PAGE_ID),
                        release);
            case ACTION_MOVE:
                return confluenceClient.moveConfluencePage(action.getJsonObject(ACTION_KEY_CONFLUENCE_PAGE),
                        action.getString(PAGE_PARENT_ID),
                        release);
        }
        return Future.failedFuture("Unknown action");
    }

    protected boolean uploadRequired(String uploadConfluencePages) {
        if (uploadConfluencePages == null) {
            getLogger().warn("Parameter {} is not set, pages upload skipped", UPLOAD_CONFLUENCE_PAGES_POINTER);
            return false;
        }

        if (UPLOAD_KEY_NONE.equalsIgnoreCase(uploadConfluencePages)) {
            getLogger().warn("Parameter {} is set to 'none', pages upload skipped", UPLOAD_CONFLUENCE_PAGES_POINTER);
            return false;
        }
        return true;
    }

    protected boolean considerToPerformAction(String uploadConfluencePages, JsonObject action) {
        String uploadPages = uploadConfluencePages.toLowerCase(Locale.ROOT);
        if (UPLOAD_KEY_ALL.equalsIgnoreCase(uploadPages)) {
            return true;
        }

        String pageTitle = getPageTitleFromAction(action);
        String pageType = (String) ACTION_POINTER_GENERATED_PAGE_TYPE.queryJson(action);

        if (isTitleMatches(pageTitle, uploadPages)) {
            return true;
        } else if (uploadPages.contains("type")) {
            Matcher matcher = Pattern.compile("type:\\s*(\\w+)").matcher(uploadPages);
            while (matcher.find()) {
                if (matcher.group(1).equalsIgnoreCase(pageType)){
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isTitleMatches(String pageTitle, String uploadPages) {
        String[] titles = uploadPages.split("\\s*,\\s*");
        return Arrays.stream(titles).anyMatch(title ->
                buildPageTitle(title).equalsIgnoreCase(pageTitle));
    }

    private JsonArray buildActions(JsonObject confluenceTree, JsonObject confluenceStructure, JsonArray generatedPages) {
        JsonArray actions = new JsonArray();
        Queue<JsonObject> queue = new LinkedList<>();

        JsonObject nextLevel = new JsonObject()
                .put(PAGE_KEY_CONFLUENCE, confluenceTree)
                .put(PAGE_KEY_STRUCTURE, confluenceStructure);
        queueChildren(queue, nextLevel, generatedPages);
        while (!queue.isEmpty()) {
            // dequeue
            JsonObject next = queue.remove();

            if (getLogger().isDebugEnabled()) {
                String pageName = (String) PAGE_POINTER_CONFLUENCE_TITLE.queryJson(next);
                if (pageName == null) {
                    pageName = (String) PAGE_POINTER_GENERATED_PAGE_TITLE.queryJson(next);
                }
                getLogger().debug("'{}': Building actions.", pageName);
            }

            actions.addAll(generateNextActions(next));

            // Add children nodes if present
            JsonArray children = (JsonArray) PAGE_POINTER_STRUCTURE_CHILDREN.queryJson(next);
            if (children != null && !children.isEmpty()) {
                queueChildren(queue, next, generatedPages);
            }
        }

        return actions;
    }

    private JsonArray generateNextActions(JsonObject next) {
        JsonArray actions = new JsonArray();
        JsonObject action = new JsonObject();
        // confluence page for that element was not found (create)
        if (!next.containsKey(PAGE_KEY_CONFLUENCE)) {
            action.put(ACTION_KEY_ACTION, ACTION_CREATE)
                    .put(ACTION_KEY_GENERATED_PAGE, PAGE_POINTER_GENERATED_PAGE.queryJson(next));
            getLogger().debug("'{}': Action {} was added to the list",
                    PAGE_POINTER_GENERATED_PAGE_TITLE.queryJson(next),
                    ACTION_CREATE);
        } else
        // generated page for that element was not found (move to trash)
        if (!next.containsKey(ACTION_KEY_GENERATED_PAGE)) {
            action.put(ACTION_KEY_ACTION, ACTION_DELETE)
                    .put(ACTION_KEY_CONFLUENCE_PAGE, PAGE_POINTER_CONFLUENCE.queryJson(next));
            getLogger().debug("'{}': Action {} was added to the list",
                    PAGE_POINTER_CONFLUENCE_TITLE.queryJson(next),
                    ACTION_DELETE);
            // Add delete actions for all the children in case it requires a move operation
            if (((JsonObject) PAGE_POINTER_CONFLUENCE.queryJson(next)).containsKey(STRUCT_CHILDREN)){
                JsonArray children = (JsonArray) PAGE_POINTER_CONFLUENCE_CHILDREN.queryJson(next);
                children.stream()
                        .map(child -> (JsonObject) child)
                        .forEach(child -> {
                            JsonObject nextChild = new JsonObject()
                                    .put(ACTION_KEY_CONFLUENCE_PARENT, next.getJsonObject(PAGE_KEY_CONFLUENCE))
                                    .put(PAGE_KEY_CONFLUENCE, child);
                            actions.addAll(generateNextActions(nextChild));
                        });
            }

        } else
        // Have new page for this existing page
        if (PAGE_POINTER_GENERATED_PAGE_ON_DISK_PATH.queryJson(next) != null) {
            action.put(ACTION_KEY_ACTION, ACTION_UPDATE)
                    .put(ACTION_KEY_CONFLUENCE_PAGE, PAGE_POINTER_CONFLUENCE.queryJson(next))
                    .put(ACTION_KEY_GENERATED_PAGE, PAGE_POINTER_GENERATED_PAGE.queryJson(next));
            getLogger().debug("'{}': Action {} was added to the list",
                    PAGE_POINTER_CONFLUENCE_TITLE.queryJson(next),
                    ACTION_UPDATE);
        } else {
            // In general all the other cases don't require any actions
            getLogger().debug("'{}': No actions were added",
                    PAGE_POINTER_CONFLUENCE_TITLE.queryJson(next));
            return actions;
        }
        action.put(PAGE_PARENT_ID, ACTION_POINTER_CONFLUENCE_PARENT_ID.queryJson(next));
        action.put(PAGE_TITLE, getPageTitleFromAction(action));
        actions.add(action);
        return actions;
    }

    /**
     * Takes next level of the structure and attempts to add the new children to the queue
     * Target structure elements have two possible fields: *type* or *title*
     *
     *
     * @param queue the queue where new children will be added
     * @param parent JsonObject containing target confluence structure (with children), confluence page (with children),
     *               confluence parent page and related generated page
     * @param generatedConfluencePages JsonArray of all generated pages
     */
    private void queueChildren(Queue<JsonObject> queue, JsonObject parent, JsonArray generatedConfluencePages) {
        JsonArray structureChildren = (JsonArray) PAGE_POINTER_STRUCTURE_CHILDREN.queryJson(parent);
        JsonArray confluenceChildren = (JsonArray) PAGE_POINTER_CONFLUENCE_CHILDREN.queryJson(parent);
        JsonArray remainingGeneratedConfluencePages = new JsonArray(generatedConfluencePages.getList());
        List<Object> remainingConfluenceChildren = new LinkedList<>();
        if (confluenceChildren != null) {
            remainingConfluenceChildren.addAll(confluenceChildren.getList());
        }

        queueTitledChildren(queue, parent, remainingConfluenceChildren, structureChildren);
        queueTypedChildren(queue, parent, remainingGeneratedConfluencePages, remainingConfluenceChildren, structureChildren);
    }

    private void queueTypedChildren(Queue<JsonObject> queue, JsonObject parent, JsonArray remainingGeneratedPages,
                                    List<Object> existingChildPages, JsonArray structChildren) {
        JsonObject existingParentPage = parent.getJsonObject(PAGE_KEY_CONFLUENCE);
        structChildren.stream()
                .map(JsonObject.class::cast)
                .filter(structChild -> structChild.containsKey(STRUCT_TYPE))
                .forEach(structChild -> {
                    // Get all generated pages of this type
                    if ( !existingChildPages.isEmpty()) {
                        // Trying to match existing pages with generated pages
                        Iterator existingChildPagesIterator = existingChildPages.iterator();
                        while (existingChildPagesIterator.hasNext()) {
                            // find the current child which fits the structure among generated pages
                            JsonObject existingChildPage = JsonObject.mapFrom(existingChildPagesIterator.next());
                            JsonObject matchedGeneratedPage = getGeneratedPage(remainingGeneratedPages,
                                    existingParentPage, existingChildPage, structChild);
                            if (matchedGeneratedPage != null) {
                                // Got the full match, update is required
                                JsonObject nextLevelChild = new JsonObject()
                                        .put(PAGE_KEY_STRUCTURE, structChild)
                                        .put(PAGE_KEY_CONFLUENCE, existingChildPage)
                                        .put(PAGE_KEY_GENERATED_PAGE, matchedGeneratedPage)
                                        .put(ACTION_KEY_CONFLUENCE_PARENT, existingParentPage);
                                queue.add(nextLevelChild);

                                remainingGeneratedPages.remove(matchedGeneratedPage);
                                existingChildPagesIterator.remove();
                            }
                        }
                    }
                    // Add remaining generated pages using the structure
                    getGeneratedPagesForStructureSection(remainingGeneratedPages, structChild,
                            (String) PAGE_POINTER_GENERATED_PAGE_TITLE.queryJson(parent))
                            .stream()
                            .forEach(generatedPage -> {
                                // Page was generated, so it should be created
                                JsonObject nextLevelChild = new JsonObject();
                                nextLevelChild
                                        .put(PAGE_KEY_STRUCTURE, structChild)
                                        .put(PAGE_KEY_GENERATED_PAGE, generatedPage)
                                        .put(ACTION_KEY_CONFLUENCE_PARENT, parent.getJsonObject(PAGE_KEY_CONFLUENCE));
                                queue.add(nextLevelChild);
                                remainingGeneratedPages.remove(generatedPage);
                            });
                });
        if (!existingChildPages.isEmpty()) {
            // Still have existing unmatched pages, to be deleted
            existingChildPages.stream()
                    .map(JsonObject::mapFrom)
                    .forEach(confluenceChild -> {
                        JsonObject nextLevelChild = new JsonObject();
                        nextLevelChild
                                .put(PAGE_KEY_CONFLUENCE, confluenceChild)
                                .put(ACTION_KEY_CONFLUENCE_PARENT, parent.getJsonObject(PAGE_KEY_CONFLUENCE));
                        queue.add(nextLevelChild);
                    });
        }
    }

    private void queueTitledChildren(Queue<JsonObject> queue, JsonObject parent, List<Object> remainingConfluenceChildren, JsonArray structChildren) {
        structChildren.stream()
                .map(structChild -> (JsonObject) structChild)
                .filter(structChild -> structChild.containsKey(PAGE_TITLE))
                .forEach(structChild -> {
                    // Searching for existing page with specified title
                    Iterator childIterator = remainingConfluenceChildren.iterator();
                    Boolean confluencePageFound = false;
                    while (childIterator.hasNext()) {
                        JsonObject confluenceChild = JsonObject.mapFrom(childIterator.next());
                        if (confluenceChild.getString(PAGE_TITLE)
                                .equals(buildPageTitle(structChild.getString(PAGE_TITLE)))) {
                            JsonObject nextLevelChild = new JsonObject()
                                    .put(PAGE_KEY_STRUCTURE, structChild)
                                    .put(PAGE_KEY_CONFLUENCE, confluenceChild)
                                    .put(PAGE_KEY_GENERATED_PAGE, new JsonObject()
                                            .put(PAGE_TITLE, structChild.getString(PAGE_TITLE)))
                                    .put(ACTION_KEY_CONFLUENCE_PARENT, parent.getJsonObject(PAGE_KEY_CONFLUENCE));
                            queue.add(nextLevelChild);
                            childIterator.remove();
                            confluencePageFound = true;
                            break;
                        }
                    }
                    if (!confluencePageFound) {
                        // No existing confluence page found, so we need to create the empty page
                        JsonObject nextLevelChild = new JsonObject()
                                .put(PAGE_KEY_STRUCTURE, structChild)
                                .put(PAGE_KEY_GENERATED_PAGE, new JsonObject()
                                        .put(PAGE_TITLE, structChild.getString(PAGE_TITLE))
                                        .put(PAGE_PARENT_TITLE, PAGE_POINTER_CONFLUENCE_TITLE.queryJson(parent)))
                                .put(ACTION_KEY_CONFLUENCE_PARENT, parent.getJsonObject(PAGE_KEY_CONFLUENCE));
                        queue.add(nextLevelChild);
                    }
                });
    }


    private JsonObject getGeneratedPage(JsonArray generatedPages, JsonObject parent, JsonObject child, JsonObject structChild) {
        JsonObject eligiblePage = (JsonObject) generatedPages.stream().filter(generatedPage ->
                        isChildAGeneratedPage(parent, child, (JsonObject) generatedPage)
                                && ((JsonObject)generatedPage).getString(STRUCT_TYPE).equals(structChild.getString(STRUCT_TYPE))
                )
                .findFirst().orElse(null);
        return eligiblePage;
    }

    private JsonArray getGeneratedPagesForStructureSection(JsonArray generatedPages, JsonObject structChild, String parentTitle) {
        List matchingPages = generatedPages.stream().filter(generatedPage ->
                        ((JsonObject) generatedPage).getString(STRUCT_TYPE).equals(structChild.getString(STRUCT_TYPE))
                                && ((JsonObject) generatedPage).getString(PAGE_PARENT_TITLE).equals(parentTitle)
                )
                .collect(Collectors.toList());
        return new JsonArray(matchingPages);
    }

    private boolean isChildAGeneratedPage(JsonObject parent, JsonObject child, JsonObject generatedPage) {
        return parent.getString(PAGE_TITLE)
                .equals(buildPageTitle(generatedPage.getString(PAGE_PARENT_TITLE)))
                && child.getString(PAGE_TITLE)
                .equals(buildPageTitle(generatedPage.getString(PAGE_TITLE)));
    }

    private Future<JsonObject> buildExistingConfluenceTree(String spaceKey, JsonObject confluenceStructure) {
        String rootPageTitle = confluenceStructure.getString(PAGE_TITLE);
        JsonArray firstLevelChildren = confluenceStructure.getJsonArray(STRUCT_CHILDREN);
        Set<String> firstLvlTitles = new HashSet<>();
        firstLevelChildren.stream().forEach(child -> {
            JsonObject childJson = (JsonObject) child;
            firstLvlTitles.add(buildPageTitle(childJson.getString(PAGE_TITLE)));
        });

        Future<JsonObject> confluenceTreeFuture = confluenceClient.getConfluencePageInfo(spaceKey, rootPageTitle)
                .compose(rootPage -> {
                    String rootId = rootPage.getString(PAGE_ID);
                    return confluenceClient.getChildPages(spaceKey, rootId)
                            .compose(children -> {
                                // Only take pages related to our release
                                JsonArray childrenList = new JsonArray();
                                children.stream().forEach(child -> {
                                    if (firstLvlTitles.contains(((JsonObject) child).getString(PAGE_TITLE))) {
                                        childrenList.add(child);
                                    }
                                });
                                rootPage.put(STRUCT_CHILDREN, childrenList);
                                return Future.succeededFuture(rootPage);
                            });
                })
                .compose(page -> {
                    List<Future> childFutures = new ArrayList<>();
                    JsonArray children = page.getJsonArray(STRUCT_CHILDREN);
                    children.stream().forEach(childPage ->
                            childFutures.add(addChildren((JsonObject) childPage, spaceKey))
                    );
                    return CompositeFuture.join(childFutures).compose(res -> Future.succeededFuture(page));
                });
        return confluenceTreeFuture;
    }

    private String buildPageTitle(String title) {
        if (title == null) {
            return null;
        }
        return release == null ? title : release + " " + title;
    }

    private Future<JsonObject> addChildren(JsonObject page, String spaceKey) {
        getLogger().debug("Adding child pages for page {}", page.getString(PAGE_TITLE));
        return confluenceClient.getChildPages(spaceKey, page.getString(PAGE_ID))
                .compose(children -> {
                    List<Future> childFutures = new ArrayList<>();
                    page.put(STRUCT_CHILDREN, children);
                    children.stream().forEach(childPage ->
                            childFutures.add(addChildren((JsonObject) childPage, spaceKey))
                    );
                    return CompositeFuture.join(childFutures).compose(res -> Future.succeededFuture(page));
                });
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
