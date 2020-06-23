package com.sibyl.httpfiledominator.mainactivity.repo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sibyl.httpfiledominator.mainactivity.model.MainModel

/**
 * @author HUANGSHI-PC on 2020-03-06 0006.
 */
class MainModelFactory(val repo: MainRepo): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainModel(repo) as T
    }
}