package com.mgd.core.gradle

/**
 * Model for the default project properties for the S3 plugin.
 */
@SuppressWarnings('FieldName')
@SuppressWarnings('AssignmentToStaticFieldFromInstanceMethod')
class S3Extension {

    private static String _profile
    private static String _endpoint
    private static String _region
    private static String _bucket
    private static Boolean _pathStyle

    static Map<String, String> getProperties() {
        return [
            profile: _profile,
            endpoint: _endpoint,
            region: _region,
            bucket: _bucket,
            usePathStyleUrl: _pathStyle
        ]
    }

    void setProfile(String profile) {
        _profile = profile
    }
    String getProfile() {
        return _profile
    }

    void setEndpoint(String endpoint) {
        _endpoint = endpoint
    }
    String getEndpoint() {
        return _endpoint
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

    void setusePathStyleUrl(Boolean pathStyle) {
        _pathStyle = pathStyle
    }
    Boolean getusePathStyleUrl() {
        return _pathStyle
    }
}
