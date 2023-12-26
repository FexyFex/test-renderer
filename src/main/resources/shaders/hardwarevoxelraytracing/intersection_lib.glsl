#define HIT_KIND_AABB 0
#define HIT_KIND_SPHERE 1

#define NODE_TYPE_PARENT 0
#define NODE_TYPE_LEAF 1

#define FALSE 0
#define TRUE 1

struct AABB {
    vec4 min;
    vec4 max;
};

struct Ray {
    vec4 origin;
    vec4 dir;
};

struct IntersectResult {
    vec3 coord;
    uint hit;
};


vec2 intersectSphere(vec3 origin, vec3 direction, AABB aabb) {
    vec3 center = aabb.min.xyz + ((aabb.max.xyz - aabb.min.xyz) / 2);
    vec3 oc = origin - center;
    float a = dot(direction, direction);
    float b = dot(oc, direction);
    float c = dot(oc, oc) - (0.5 * 0.5);
    float discr = b * b - a * c;

    vec2 t = vec2(-1.0, -1.0);
    if (discr >= 0.0) {
        float t0 = (-b - sqrt(discr)) / a;
        float t1 = (-b + sqrt(discr)) / a;
        t = vec2(t0, t1);
    }

    return t;
}

vec2 intersectAABB(vec3 origin, vec3 direction, AABB aabb) {
    vec3  invDir = 1.0 / direction;
    vec3  tbot   = invDir * (aabb.min.xyz - origin);
    vec3  ttop   = invDir * (aabb.max.xyz - origin);
    vec3  tmin   = min(ttop, tbot);
    vec3  tmax   = max(ttop, tbot);
    float t0     = max(tmin.x, max(tmin.y, tmin.z));
    float t1     = min(tmax.x, min(tmax.y, tmax.z));
    return t1 > max(t0, 0.0) ? vec2(t0, t1) : vec2(-1.0, -1.0);
}

// Returns the 3D coords of the intersection
const uint RIGHT = 0;
const uint LEFT = 1;
const uint MIDDLE = 2;
IntersectResult getIntersectionRayToAABB(Ray ray, AABB aabb) {
    IntersectResult result;
    bool startsInside = true;
    uvec3 quadrant;
    vec3 candidatePlane;
    vec3 maxT;
    uint whichPlane;

    for (int i = 0; i < 3; i++) {
        if (ray.origin[i] < aabb.min[i]) {
            quadrant[i] = LEFT;
            candidatePlane[i] = aabb.min[i];
            startsInside = false;
        } else if (ray.origin[i] > aabb.max[i]) {
            quadrant[i] = RIGHT;
            candidatePlane[i] = aabb.max[i];
            startsInside = false;
        } else {
            quadrant[i] = MIDDLE;
        }
    }

    if (startsInside) {
        result.coord = ray.origin.xyz;
        result.hit = TRUE;
        return result;
    }

    for (int i = 0; i < 3; i++) {
        if (quadrant[i] != MIDDLE && ray.dir[i] != 0.0) {
            maxT[i] = (candidatePlane[i] - ray.origin[i]) / ray.dir[i];
        } else {
            maxT[i] = -1.0;
        }
    }

    for (int i = 1; i < 3; i++) {
        if (maxT[whichPlane] < maxT[i])
            whichPlane = i;
    }

    if (maxT[whichPlane] < 0.0) {
        result.hit = FALSE;
        return result;
    }

    for (int i = 0; i < 3; i++) {
        if (whichPlane != i) {
            result.coord[i] = ray.origin[i] + maxT[whichPlane] * ray.dir[i];
            if (result.coord[i] < aabb.min[i] || result.coord[i] > aabb.max[i]) {
                result.hit = FALSE;
                return result;
            } else {
                result.coord[i] = candidatePlane[i];
            }
        }
    }

    result.hit = TRUE;
    return result;
}