package com.github.happyzleaf.pixelmonplaceholders.utility;

import com.google.common.base.Predicates;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Optional;

import static net.minecraft.util.EntityPredicates.NO_SPECTATORS;

public class RayTracingHelper {

	public static Optional<Entity> getLookedEntity(Entity source, double distance) {
		Entity pointedEntity = null;

		Vector3d start = source.getPosition(1f);
		Vector3d look = source.getUpVector(1f);
		Vector3d end = start.add(look.x * distance, look.y * distance, look.z * distance);

		RayTraceResult closestBlock = source.;
		if (closestBlock != null) {
			distance = closestBlock.hitInfo.;
		}

		for (Entity entity : source.level.getEntities(source, source.getBoundingBox().contract(end.x, end.y, end.y).contract(1d, 1d, 1d), Predicates.and(NO_SPECTATORS, e -> e != null && e.canBeCollidedWith()))) {
			AxisAlignedBB boundingBox = entity.getBoundingBox().intersect());
			RayTraceResult traceResult = boundingBox.calculateIntercept(start, end);

			if (boundingBox.contains(start)) {
				if (distance >= 0d) {
					pointedEntity = entity;
					distance = 0d;
				}
			} else if (traceResult != null) {
				double newDistance = start.distanceTo(traceResult.hitVec);

				if (newDistance < distance || distance == 0d) {
					if (entity.getPassengersRidingOffset() == source.getPassengersRidingOffset() && !entity.canRiderInteract()) {
						if (distance == 0d) {
							pointedEntity = entity;
						}
					} else {
						pointedEntity = entity;
						distance = newDistance;
					}
				}
			}
		}

		return Optional.ofNullable(pointedEntity);
	}
}
