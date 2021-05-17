package com.harsha.callrecorder.object;

import io.realm.RealmObject;
import io.realm.annotations.Ignore;

public class IncomingCallObject extends RealmObject {
    private String mPhoneNumber;

    private long mBeginTimestamp, mEndTimestamp;

    private boolean mIsInProgress;

    // Telephony "SIM" data
    private String mSimOperator, mSimOperatorName, mSimCountryIso;
    private String mSimSerialNumber;
    // - Telephony "SIM" data

    // Telephony "Network" data
    private String mNetworkOperator, mNetworkOperatorName, mNetworkCountryIso;
    // - Telephony "Network" data

    // Media recorder information
    private int mAudioSource, mOutputFormat, mAudioEncoder;
    private String mOutputFile;
    // - Media recorder information

    private boolean mIsSaved;

    // Constructor(s)
    public IncomingCallObject() {
        // Default object empty public constructor (for field(s) setting using setter(s))
    }

    public IncomingCallObject(String phoneNumber) {
        mPhoneNumber = phoneNumber;
    }

    public IncomingCallObject(String phoneNumber,
                              long beginTimestamp, long endTimestamp,
                              boolean isInProgress,
                              String simOperator, String simOperatorName, String simCountryIso,
                              String simSerialNumber,
                              String networkOperator, String networkOperatorName, String networkCountryIso,
                              int audioSource, int outputFormat, int audioEncoder,
                              String outputFile,
                              boolean isSaved) {
        mPhoneNumber = phoneNumber;

        mBeginTimestamp = beginTimestamp;
        mEndTimestamp = endTimestamp;

        mIsInProgress = isInProgress;

        // Telephony "SIM" data
        mSimOperator = simOperator;
        mSimOperatorName = simOperatorName;
        mSimCountryIso = simCountryIso;
        mSimSerialNumber = simSerialNumber;
        // - Telephony "SIM" data

        // Telephony "Network" data
        mNetworkOperator = networkOperator;
        mNetworkOperatorName = networkOperatorName;
        mNetworkCountryIso = networkCountryIso;
        // - Telephony "Network" data

        // Media recorder information
        mAudioSource = audioSource;
        mOutputFormat = outputFormat;
        mAudioEncoder = audioEncoder;
        mOutputFile = outputFile;
        // - Media recorder information

        mIsSaved = isSaved;
    }
    // - Constructor(s)

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        mPhoneNumber = phoneNumber;
    }

    public long getBeginTimestamp() {
        return mBeginTimestamp;
    }

    public void setBeginTimestamp(long beginTimestamp) {
        mBeginTimestamp = beginTimestamp;
    }

    public long getEndTimestamp() {
        return mEndTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        mEndTimestamp = endTimestamp;
    }

    public boolean isIsInProgress() {
        return mIsInProgress;
    }

    public void setIsInProgress(boolean isInProgress) {
        mIsInProgress = isInProgress;
    }

    // Telephony "SIM" data
    public String getSimOperator() {
        return mSimOperator;
    }

    public void setSimOperator(String simOperator) {
        mSimOperator = simOperator;
    }

    public String getSimOperatorName() {
        return mSimOperatorName;
    }

    public void setSimOperatorName(String simOperatorName) {
        mSimOperatorName = simOperatorName;
    }

    public String getSimCountryIso() {
        return mSimCountryIso;
    }

    public void setSimCountryIso(String simCountryIso) {
        mSimCountryIso = simCountryIso;
    }

    public String getSimSerialNumber() {
        return mSimSerialNumber;
    }

    public void setSimSerialNumber(String simSerialNumber) {
        mSimSerialNumber = simSerialNumber;
    }
    // - Telephony "SIM" data

    // Telephony "Network" data
    public String getNetworkOperator() {
        return mNetworkOperator;
    }

    public void setNetworkOperator(String networkOperator) {
        mNetworkOperator = networkOperator;
    }

    public String getNetworkOperatorName() {
        return mNetworkOperatorName;
    }

    public void setNetworkOperatorName(String networkOperatorName) {
        mNetworkOperatorName = networkOperatorName;
    }

    public String getNetworkCountryIso() {
        return mNetworkCountryIso;
    }

    public void setNetworkCountryIso(String networkCountryIso) {
        mNetworkCountryIso = networkCountryIso;
    }
    // - Telephony "Network" data

    // Media recorder information
    public int getAudioSource() {
        return mAudioSource;
    }

    public void setAudioSource(int audioSource) {
        mAudioSource = audioSource;
    }

    public int getOutputFormat() {
        return mOutputFormat;
    }

    public void setOutputFormat(int outputFormat) {
        mOutputFormat = outputFormat;
    }

    public int getAudioEncoder() {
        return mAudioEncoder;
    }

    public void setAudioEncoder(int audioEncoder) {
        mAudioEncoder = audioEncoder;
    }

    public String getOutputFile() {
        return mOutputFile;
    }

    public void setOutputFile(String outputFile) {
        mOutputFile = outputFile;
    }
    // - Media recorder information

    public boolean getIsSaved() {
        return mIsSaved;
    }

    public void setIsSaved(boolean isSaved) {
        mIsSaved = isSaved;
    }

    // ----

    @Ignore
    private boolean mIsHeader = false;

    @Ignore
    private String mHeaderTitle = null;

    public IncomingCallObject (boolean isHeader, String headerTitle) {
        mIsHeader = isHeader;

        mHeaderTitle = headerTitle;
    }


    public boolean getIsHeader() {
        return mIsHeader;
    }

    public void setIsHeader(boolean isHeader) {
        mIsHeader = isHeader;
    }

    public String getHeaderTitle() {
        return mHeaderTitle;
    }

    public void setHeaderTitle(String headerTitle) {
        mHeaderTitle = headerTitle;
    }

    // ----

    @Ignore
    private boolean mIsLastInCategory = false;

    public boolean getIsLastInCategory() {
        return mIsLastInCategory;
    }

    public void setIsLastInCategory(boolean isLastInCategory) {
        mIsLastInCategory = isLastInCategory;
    }

    // ----

    @Ignore
    private String mCorrespondentName = null;

    public String getCorrespondentName() {
        return mCorrespondentName;
    }

    public void setCorrespondentName(String correspondentName) {
        mCorrespondentName = correspondentName;
    }
}
