package com.mgd.core.gradle

/**
 * Model for the default project properties for the S3 plugin.
 */
class S3Extension {

    private static String _profile
    private static String _region
    private static String _bucket

    void setProfile(String profile) {
        _profile = profile
    }
    String getProfile() {
        return _profile
    }

    void setRegion(String region) {
        _region = region
    }
    String getRegion() {
        return _region
    }

    void setBucket(String bucket) {
        _bucket = bucket
    }
    String getBucket() {
        return _bucket
    }

    static Map<String, String> getProperties() {
        return [
                profile: _profile,
                region: _region,
                bucket: _bucket
        ]
    }
}
