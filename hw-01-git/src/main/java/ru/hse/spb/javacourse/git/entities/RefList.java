package ru.hse.spb.javacourse.git.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import ru.hse.spb.javacourse.git.command.GitCommandException;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static ru.hse.spb.javacourse.git.entities.RepositoryManager.GIT_REFS_PATH;
import static ru.hse.spb.javacourse.git.entities.RepositoryManager.HEAD_REF_NAME;
import static ru.hse.spb.javacourse.git.entities.RepositoryManager.currentBranch;

public class RefList {
    @NotNull private Map<String, ArrayList<String>> refs;
    @Nullable private String headRef;

    public RefList() throws IOException {
        final List<Object> refsList = new JSONArray(Files.lines(GIT_REFS_PATH).collect(Collectors.joining(","))).toList();
        refs = refsList
                .stream()
                .map(o -> new JSONObject((HashMap) o))
                .map(Ref::new)
                .filter(ref -> {
                    if (ref.getName().equals(HEAD_REF_NAME)) {
                        headRef = ref.getRevision();
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.groupingBy(Ref::getRevision, Collectors.mapping(Ref::getName, Collectors.toCollection(ArrayList::new))));
        if (headRef != null) {
            currentBranch = headRef;
        }
    }

    public final @Nullable String getRevisionForRef(@NotNull String refName) {
        String refToFound = refName.equals(HEAD_REF_NAME) ? headRef : refName;
        final Map.Entry<String, ArrayList<String>> refRecord = refs.entrySet()
                .stream()
                .filter(e -> e.getValue().contains(refToFound))
                .findFirst()
                .orElse(null);
        return refRecord == null ? null : refRecord.getKey();
    }

    public @NotNull List<String> getRefsToRevision(@NotNull String revision) {
        final ArrayList<String> refsToRevision = refs.getOrDefault(revision, new ArrayList<>());
        if (revision.equals(getRevisionReferencedFromHead())) {
            refsToRevision.add(HEAD_REF_NAME);
        }
        return refsToRevision;
    }

    public void add(@NotNull Ref ref) {
        if (!refs.containsKey(ref.getRevision())) {
            refs.put(ref.getRevision(), new ArrayList<>());
        }
        final List<String> revisions = refs.get(ref.getRevision());
        revisions.add(ref.getName());
    }

    public void update(@NotNull String refName, @NotNull String revision) {
        if (refName.equals(HEAD_REF_NAME)) {
            headRef = revision;
        } else {
            remove(refName);
            add(new Ref(refName, revision));
        }
    }

    public void remove(@NotNull String refName) {
        refs.forEach((revisionName, refsList) -> refsList.remove(refName));
    }

    @NotNull
    public Set<String> getAllReferencedRevisions() {
        return refs.keySet();
    }

    @Nullable
    public String getHeadRef() {
        return headRef;
    }

    @Nullable
    public String getRevisionReferencedFromHead() {
        return headRef == null ? null : getRevisionForRef(headRef);
    }

    public void write() throws IOException {
        JSONArray refsJson = new JSONArray();
        refs.forEach((revisionName, refsList) -> {
            refsList.forEach(ref -> {
                JSONObject refJson = new JSONObject();
                refJson.put("name", ref);
                refJson.put("revision", revisionName);
                refsJson.put(refJson);
            });
        });
        JSONObject refJson = new JSONObject();
        refJson.put("name", HEAD_REF_NAME);
        refJson.put("revision", headRef);
        refsJson.put(refJson);
        Files.write(GIT_REFS_PATH, Collections.singletonList(refsJson.toString()));
    }
}
