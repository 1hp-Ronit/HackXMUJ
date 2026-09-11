package com.pulsenet.app.di

import com.pulsenet.app.mesh.MeshTransport
import com.pulsenet.app.mesh.NearbyMeshManager
import com.pulsenet.app.security.KeyManager
import com.pulsenet.app.security.KeySigner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun bindKeySigner(keyManager: KeyManager): KeySigner

    @Binds
    abstract fun bindMeshTransport(nearbyMeshManager: NearbyMeshManager): MeshTransport
}
