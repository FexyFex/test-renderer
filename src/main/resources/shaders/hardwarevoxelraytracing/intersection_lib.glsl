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