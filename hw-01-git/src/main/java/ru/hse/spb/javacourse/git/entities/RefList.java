package ru.hse.spb.javacourse.git.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import static ru.hse.spb.javacourse.git.entities.RepositoryManager.GIT_REFS_PATH;

public class RefList {
    @NotNull private Map<String, ArrayList<String>> refs;

    public RefList() throws IOException {
        final List<Object> list = new JSONArray(Files.lines(GIT_REFS_PATH).collect(Collectors.joining(","))).toList();
        refs = list
                .stream()
                .map(o -> new JSONObject((HashMap) o))
                .map(Ref::new)
                .collect(Collectors.groupingBy(Ref::getRevision, Collectors.mapping(Ref::getName, Collectors.toCollection(ArrayList::new))));
    }

    public @Nullable String getRevisionForRef(@NotNull String refName) {
        final Map.Entry<String, ArrayList<String>> refRecord = refs.entrySet()
                .stream()
                .filter(e -> e.getValue().contains(refName))
                .findFirst()
                .orElse(null);
        return refRecord == null ? null : refRecord.getKey();
    }

    public @NotNull List<String> getRefsToRevision(@NotNull String revision) {
        return refs.getOrDefault(revision, new ArrayList<>());
    }

    public void add(@NotNull Ref ref) {
        if (!refs.containsKey(ref.getRevision())) {
            refs.put(ref.getRevision(), new ArrayList<>());
        }
        final List<String> revisions = refs.get(ref.getRevision());
        revisions.add(ref.getName());
    }

    public void update(@NotNull String refName, @NotNull String revision) {
        remove(refName);
        add(new Ref(refName, revision));
    }

    public void remove(@NotNull String refName) {
        refs.forEach((revisionName, refsList) -> refsList.remove(refName));
    }

    @NotNull
    public Set<String> getAllReferencedRevisions() {
        return refs.keySet();
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
        Files.write(GIT_REFS_PATH, Collections.singletonList(refsJson.toString()));
    }
}
