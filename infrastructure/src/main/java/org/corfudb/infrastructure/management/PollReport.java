package org.corfudb.infrastructure.management;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.corfudb.common.result.Result;
import org.corfudb.protocols.wireprotocol.ClusterState;
import org.corfudb.runtime.exceptions.WrongEpochException;
import org.corfudb.runtime.view.Layout;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

/**
 * Poll Report generated by the detectors that poll to detect failed or healed nodes.
 * This is consumed and analyzed by the Management Server to detect changes in the cluster and
 * take appropriate action.
 * Created by zlokhandwala on 3/21/17.
 */
@Builder
@Slf4j
@Getter
@ToString
public class PollReport {

    @Default
    private final long pollEpoch = Layout.INVALID_EPOCH;

    /**
     * A map of nodes answered with {@link WrongEpochException}.
     */
    @Default
    private final ImmutableMap<String, Long> wrongEpochs = ImmutableMap.of();

    /**
     * Current cluster state, collected by a failure detector
     */
    @NonNull
    private final Result<ClusterState, IllegalStateException> clusterState;

    /**
     * Time spent on collecting this report
     */
    @NonNull
    private final Duration elapsedTime;

    /**
     * Returns all connected nodes to the current node including nodes with higher epoch.
     *
     * @return set of all reachable nodes
     */
    public ImmutableSet<String> getAllReachableNodes() {
        return Sets.union(getReachableNodes(), wrongEpochs.keySet()).immutableCopy();
    }

    /**
     * Contains list of servers successfully connected with current node.
     * It doesn't contain nodes answered with WrongEpochException.
     */
    public Set<String> getReachableNodes() {
        if (clusterState.isError()) {
            return ImmutableSet.of();
        }

        Set<String> connectedNodes = clusterState.get()
                .getLocalNodeConnectivity()
                .getConnectedNodes();
        return Sets.difference(connectedNodes, wrongEpochs.keySet());
    }

    /**
     * Contains list of servers disconnected from this node. If the node A can't ping node B then node B will be added
     * to failedNodes list.
     */
    public Set<String> getFailedNodes() {
        if (clusterState.isError()) {
            return ImmutableSet.of();
        }

        Set<String> failedNodes = clusterState.get()
                .getLocalNodeConnectivity()
                .getFailedNodes();

        return Sets.difference(failedNodes, wrongEpochs.keySet());
    }

    /**
     * Check to see if the current epoch/register has been decided/filled. We do this by
     * comparing the epoch of the latest committed layout and the epoch associated with the
     * {@link WrongEpochException}.
     *
     * @return Optional.of(epoch) if latest layout slot is vacant, Optional.none() otherwise.
     */
    public Optional<Long> getLayoutSlotUnFilled(@NonNull Layout latestCommittedLayout) {
        log.trace("getLayoutSlotUnFilled. Wrong epochs: {}, latest layout epoch: {}",
                wrongEpochs, latestCommittedLayout.getEpoch()
        );

        long maxOutOfPhaseEpoch = wrongEpochs.values().stream()
                .max(Long::compare)
                .orElse(latestCommittedLayout.getEpoch());
        if (maxOutOfPhaseEpoch > latestCommittedLayout.getEpoch()) {
            return Optional.of(maxOutOfPhaseEpoch);
        }

        return Optional.empty();
    }
}
