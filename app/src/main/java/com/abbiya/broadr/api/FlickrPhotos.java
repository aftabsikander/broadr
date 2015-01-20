/*
 * Copyright (C) 2014 Pat Dalberg 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.abbiya.broadr.api;

import com.google.gson.annotations.Expose;

import java.util.List;

public class FlickrPhotos {
    @Expose
    public int page;
    @Expose
    public String pages;
    @Expose
    public int perpage;
    @Expose
    public String total;
    @Expose
    public List<FlickrPhoto> photo;

    public List<FlickrPhoto> getPhotos() {
        return this.photo;
    }
}