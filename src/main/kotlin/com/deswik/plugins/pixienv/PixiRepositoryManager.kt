package com.deswik.plugins.pixienv

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.errorProcessing.PyResult
import com.jetbrains.python.packaging.PyPackageVersion
import com.jetbrains.python.packaging.PyPackageVersionNormalizer
import com.jetbrains.python.packaging.PyRequirement
import com.jetbrains.python.packaging.common.PythonPackageDetails
import com.jetbrains.python.packaging.common.PythonRepositoryPackageSpecification
import com.jetbrains.python.packaging.management.PythonRepositoryManager
import com.jetbrains.python.packaging.repository.PyPackageRepository

/*
A stub implementation of RepositoryManager
Because PythonRepositoryManager is marked as unstable, it does not make too much sense to implement it outright
 */
@Suppress("UnstableApiUsage")
class PixiRepositoryManager(override val project: Project, val sdk: Sdk) : PythonRepositoryManager {
    override val repositories: List<PyPackageRepository>
        get() = emptyList()

    override fun allPackages(): Set<String> {
        return emptySet()
    }

    override fun searchPackages(query: String): Map<PyPackageRepository, List<String>> {
        TODO("Not yet implemented")
    }

    override fun searchPackages(query: String, repository: PyPackageRepository): List<String> {
        TODO("Not yet implemented")
    }

    override suspend fun findPackageSpecification(requirement: PyRequirement, repository: PyPackageRepository?): PythonRepositoryPackageSpecification? {
        TODO("Not yet implemented")
    }

    override suspend fun getPackageDetails(packageName: String, repository: PyPackageRepository?): PyResult<PythonPackageDetails> {
        TODO("Not yet implemented")
    }

    override suspend fun getLatestVersion(packageName: String, repository: PyPackageRepository?): PyPackageVersion? {
        return PyPackageVersionNormalizer.normalize("")
    }

    override suspend fun getVersions(packageName: String, repository: PyPackageRepository?): List<String>? {
        return emptyList()
    }

    override suspend fun refreshCaches() {
    }

    override suspend fun initCaches() {
    }
}
