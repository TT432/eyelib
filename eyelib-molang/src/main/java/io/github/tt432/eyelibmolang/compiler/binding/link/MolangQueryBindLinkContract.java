package io.github.tt432.eyelibmolang.compiler.binding.link;

import io.github.tt432.eyelibmolang.compiler.binding.BoundMolang;
import io.github.tt432.eyelibmolang.mapping.api.MolangFunction;
import io.github.tt432.eyelibmolang.mapping.api.MolangMappingTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MolangQueryBindLinkContract {
    private MolangQueryBindLinkContract() {
    }

    public record QueryBindLinkRequest(
            String symbolicQueryName,
            BoundMolang.BoundQueryAccessExpr.QueryProjectionKind querySurfaceKind,
            List<MolangMappingTree.VisibleArgumentKind> visibleCallShape
    ) {
        public QueryBindLinkRequest {
            Objects.requireNonNull(symbolicQueryName, "symbolicQueryName");
            Objects.requireNonNull(querySurfaceKind, "querySurfaceKind");
            Objects.requireNonNull(visibleCallShape, "visibleCallShape");
            visibleCallShape = Collections.unmodifiableList(new ArrayList<>(visibleCallShape));
        }
    }

    public record CandidateSetRef(String value) {
        public CandidateSetRef {
            Objects.requireNonNull(value, "value");
        }
    }

    public record RegistryVersionRef(String value) {
        public RegistryVersionRef {
            Objects.requireNonNull(value, "value");
        }
    }

    public record CandidateRef(String value) {
        public CandidateRef {
            Objects.requireNonNull(value, "value");
        }
    }

    public record QueryCandidateDescriptor(
            CandidateRef candidateRef,
            Set<MolangFunction.ParameterRole> requiredHostRoles,
            MolangMappingTree.FunctionInfo callableDescriptor
    ) {
        public QueryCandidateDescriptor {
            Objects.requireNonNull(candidateRef, "candidateRef");
            Objects.requireNonNull(requiredHostRoles, "requiredHostRoles");
            Objects.requireNonNull(callableDescriptor, "callableDescriptor");
            requiredHostRoles = Collections.unmodifiableSet(new LinkedHashSet<>(requiredHostRoles));
        }
    }

    public record QueryLinkResult(
            CandidateSetRef candidateSetRef,
            RegistryVersionRef registryVersionRef,
            String symbolicQueryName,
            BoundMolang.BoundQueryAccessExpr.QueryProjectionKind querySurfaceKind,
            List<MolangMappingTree.VisibleArgumentKind> visibleCallShape,
            List<QueryCandidateDescriptor> candidates
    ) {
        public QueryLinkResult {
            Objects.requireNonNull(candidateSetRef, "candidateSetRef");
            Objects.requireNonNull(registryVersionRef, "registryVersionRef");
            Objects.requireNonNull(symbolicQueryName, "symbolicQueryName");
            Objects.requireNonNull(querySurfaceKind, "querySurfaceKind");
            Objects.requireNonNull(visibleCallShape, "visibleCallShape");
            Objects.requireNonNull(candidates, "candidates");
            visibleCallShape = Collections.unmodifiableList(new ArrayList<>(visibleCallShape));
            candidates = List.copyOf(candidates);
        }
    }
}
