package net.engawapg.app.camrepo.settings

import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.engawapg.app.camrepo.R

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        val versionPref = findPreference<Preference>(getString(R.string.pref_key_version))
        versionPref?.summaryProvider = Preference.SummaryProvider<Preference> {
            activity?.packageName?.let {
                activity?.packageManager?.getPackageInfo(it, 0)
            }?.versionName.toString()
        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.setTitle(R.string.settings)
    }
}
