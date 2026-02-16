package com.gmail.vondenuelle.denuspend.utils.media

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


//Which options is selected in view(e.g. ID, proof of address, signature photo)
enum class SelectedOption(val value: String) {
    SELECTED_ID("SELECTED_ID"),
    SELECTED_PROOF_OF_ADDRESS("SELECTED_PROOF_OF_ADDRESS"),

    SELECTED_FILES("SELECTED_FILES"),
    SELECTED_SINGLE_FILE("SELECTED_SINGLE_FILE")
}


@HiltViewModel
class MediaPickerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val gallerySaver: GallerySaver
) : ViewModel() {
    companion object {
        private const val MEDIA_PICKER_UI_STATE = "MEDIA_PICKER_UI_STATE"
    }

    private val _uriStateFlow = MutableStateFlow<MediaPickerUiState>(MediaPickerUiState())
    val uriStateFlow = _uriStateFlow.asStateFlow()

    //Temporary uri for holder of empty uri but contains data for storing captured image
    private var tempCameraUri: Uri? = null
    fun getTempCameraUri() : Uri? {
        return tempCameraUri
    }
    fun setTempPendingCameraUri(uri: Uri) {
        tempCameraUri = uri
    }
    fun confirmCameraResultAndSave() {
        tempCameraUri?.let { setMediaUri(it) }
        tempCameraUri = null
    }

    //Upon initialization of fragment
    init {
        // Get the saved state from SavedStateHandle and update the Flow state
        val savedState = savedStateHandle.get<MediaPickerUiState>("MEDIA_PICKER_UI_STATE")

        savedState?.let {
            _uriStateFlow.update { it } // Set the restored state if it exists
        }
    }

    // Save the current state to SavedStateHandle
    private fun saveState() {
        savedStateHandle[MEDIA_PICKER_UI_STATE] = _uriStateFlow.value
    }

    // Uri type = content://
    // Set uri based on selected option, like address or upload id
    //this is needed because MediaPickerHelper only provides uri
    //to make it easier to handle, fragment or activity will handle which uri should be saved to which holder
    //if more uploads, then add as needed
    fun setMediaUri(uri: Uri) {
        _uriStateFlow.update { state ->
            when (state.selectedOption) {
                SelectedOption.SELECTED_ID -> state.copy(idUri = uri)
                SelectedOption.SELECTED_PROOF_OF_ADDRESS -> state.copy(addressUri = uri)
                SelectedOption.SELECTED_FILES -> {
                    val files = state.files.toMutableList()
                    files.add(uri)

                    state.copy(files = files.toList(), fileHolder =  uri)
                }
                SelectedOption.SELECTED_SINGLE_FILE -> {
                    state.copy(singleFile = uri)
                }
                else -> state
            }
        }
        saveState() // Save the updated state after change
    }

    /**
     * Useful if you have a list in media picker and you wanna remove it
     */
    fun removeDataFromList(option : SelectedOption, index : Int) {
        //List the possible options whenever you have a list here
        _uriStateFlow.update { state ->
            when (option) {
                SelectedOption.SELECTED_FILES -> {
                    val files = state.files.toMutableList()
                    files.removeAt(index)

                    state.copy(files = files.toList())
                }
                else -> state
            }
        }
    }
    /**
     * Useful if you you wanna remove a single media
     */
    fun removeData(option : SelectedOption) {
        //List the possible options whenever you wanna remove something
        _uriStateFlow.update { state ->
            when (option) {
                SelectedOption.SELECTED_SINGLE_FILE -> {
                    state.copy(singleFile = null)
                }
                else -> state
            }
        }
    }

    fun setSelectedOption(selected: SelectedOption) {
        _uriStateFlow.update { state ->
            state.copy(selectedOption = selected)
        }
        saveState() // Save the updated state
    }

    /**
     * A helper function to return specific Uri if needed based on provided selected option
     */
    fun getSpecificUri(): Uri? {
        return when (_uriStateFlow.value.selectedOption) {
            SelectedOption.SELECTED_ID -> _uriStateFlow.value.idUri
            SelectedOption.SELECTED_PROOF_OF_ADDRESS -> _uriStateFlow.value.addressUri
            SelectedOption.SELECTED_SINGLE_FILE -> _uriStateFlow.value.singleFile

            SelectedOption.SELECTED_FILES -> _uriStateFlow.value.fileHolder
            else -> null
        }
    }

    /**
     * save to gallery and return the saved uri
     */
    fun saveToGallery(uri : Uri?) {
        uri?.let {
            viewModelScope.launch {
                val newUri = gallerySaver.saveImageToGallery(it)
                _uriStateFlow.update { flow -> flow.copy(savedToGalleryUri = newUri) }
            }
        }

        //reset temp uri of whichever is the selected option and the one that user opted to save to gallery
        when (_uriStateFlow.value.selectedOption) {
            SelectedOption.SELECTED_ID -> _uriStateFlow.update { it.copy(idUri = null) }
            SelectedOption.SELECTED_PROOF_OF_ADDRESS ->  _uriStateFlow.update { it.copy(addressUri = null) }
            SelectedOption.SELECTED_SINGLE_FILE -> _uriStateFlow.update { it.copy(singleFile = null) }
            SelectedOption.SELECTED_FILES -> _uriStateFlow.update { it.copy(fileHolder = null) }
            else -> null
        }
    }
}