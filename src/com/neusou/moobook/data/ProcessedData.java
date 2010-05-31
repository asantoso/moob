package com.neusou.moobook.data;

import java.util.ArrayList;

public class ProcessedData {
		
		public boolean isDirty = true;
		public Attachment mAttachment;		
		public int numMediaImages;		
		public String mProfileImageUri = null;
		public static int MAX_ATTACHMENTS = 5;
		
		public String mediaImagesSrcs[] = new String[MAX_ATTACHMENTS];
		public String mediaImagesHref[] = new String[MAX_ATTACHMENTS];
		
		public static String LOG_TAG = "ProcessedAttachmentData";
		
		public void clear(){
			//TODO [Object Pools] ProcessedAttachmentData release
			mAttachment.clear();
			mAttachment = null;
		}
		
		public static ProcessedData process(Attachment attachment){
			//TODO [Object pools] ProcessedAttachmentData acquire
			ProcessedData processedData = new ProcessedData();
			processedData.mAttachment = attachment;
			
			if(attachment.mAttachmentMediaList != null){
			
			int numMedias = attachment.mAttachmentMediaList.size();
		
			if(attachment.mAttachmentMediaList.size() > 0){
				
				//Logger.l(Logger.DEBUG, LOG_TAG, "[doInBackground] getting media srcs");
				
				switch(attachment.mMediaType){
					case AttachmentMedia.PHOTO:{
						ArrayList<AttachmentPhoto> medias = ((ArrayList<AttachmentPhoto>) attachment.mAttachmentMediaList);
						int max = Math.min(MAX_ATTACHMENTS, numMedias);
						for(int i=0;i<max;i++){
							processedData.mediaImagesSrcs[i] = medias.get(i).src;
						//	Logger.l(Logger.DEBUG,"Attachment",i+" photo media src: "+medias.get(i).src);
						}
						processedData.numMediaImages = numMedias;
						break;
					}
					case AttachmentMedia.VIDEO:{
						ArrayList<AttachmentVideo> medias = ((ArrayList<AttachmentVideo>) attachment.mAttachmentMediaList);
						int max = Math.min(MAX_ATTACHMENTS, numMedias);
						for(int i=0;i<max;i++){
							processedData.mediaImagesSrcs[i] = medias.get(i).src;
						//	Logger.l(Logger.DEBUG,"Attachment",i+" video media display url: "+medias.get(i).src);
						}
						processedData.numMediaImages = numMedias;
						break;
					}				
					case AttachmentMedia.LINK:{
						ArrayList<AttachmentLink> medias = ((ArrayList<AttachmentLink>) attachment.mAttachmentMediaList);
						//TODO Attachment #images to show
						int max = Math.min(MAX_ATTACHMENTS, numMedias);
						for(int i=0;i<max;i++){
							//Logger.l(Logger.DEBUG, LOG_TAG, " mediaImagesSrcs length:"+processedData.mediaImagesSrcs.length+", medias size: "+medias.size());
							processedData.mediaImagesSrcs[i] = medias.get(i).src;
						//	Logger.l(Logger.DEBUG,"Attachment",i+" video media display url: "+medias.get(i).src);
						}
						processedData.numMediaImages = numMedias;
						break;
					}
				}//end switch							
			}
		}//end if
		return processedData;
		}
}


	
