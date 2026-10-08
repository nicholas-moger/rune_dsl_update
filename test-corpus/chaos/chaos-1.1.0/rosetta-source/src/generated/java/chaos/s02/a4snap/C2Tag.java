package chaos.s02.a4snap;

import chaos.s02.a4snap.meta.C2TagMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.Required;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * Helper the import-style axis relocates.
 * @version 1.0.0-SNAPSHOT
 */
@RosettaDataType(value="C2Tag", builder=C2Tag.C2TagBuilderImpl.class, version="1.0.0-SNAPSHOT")
@RuneDataType(value="C2Tag", model="chaos", builder=C2Tag.C2TagBuilderImpl.class, version="1.0.0-SNAPSHOT")
public interface C2Tag extends RosettaModelObject {

	C2TagMeta metaData = new C2TagMeta();

	/*********************** Getter Methods  ***********************/
	String getTagLine();

	/*********************** Build Methods  ***********************/
	C2Tag build();
	
	C2Tag.C2TagBuilder toBuilder();
	
	static C2Tag.C2TagBuilder builder() {
		return new C2Tag.C2TagBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C2Tag> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C2Tag> getType() {
		return C2Tag.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("tagLine"), String.class, getTagLine(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C2TagBuilder extends C2Tag, RosettaModelObjectBuilder {
		C2Tag.C2TagBuilder setTagLine(String tagLine);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("tagLine"), String.class, getTagLine(), this);
		}
		

		C2Tag.C2TagBuilder prune();
	}

	/*********************** Immutable Implementation of C2Tag  ***********************/
	class C2TagImpl implements C2Tag {
		private final String tagLine;
		
		protected C2TagImpl(C2Tag.C2TagBuilder builder) {
			this.tagLine = builder.getTagLine();
		}
		
		@Override
		@RosettaAttribute("tagLine")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("tagLine")
		public String getTagLine() {
			return tagLine;
		}
		
		@Override
		public C2Tag build() {
			return this;
		}
		
		@Override
		public C2Tag.C2TagBuilder toBuilder() {
			C2Tag.C2TagBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C2Tag.C2TagBuilder builder) {
			ofNullable(getTagLine()).ifPresent(builder::setTagLine);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C2Tag _that = getType().cast(o);
		
			if (!Objects.equals(tagLine, _that.getTagLine())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tagLine != null ? tagLine.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C2Tag {" +
				"tagLine=" + this.tagLine +
			'}';
		}
	}

	/*********************** Builder Implementation of C2Tag  ***********************/
	class C2TagBuilderImpl implements C2Tag.C2TagBuilder {
	
		protected String tagLine;
		
		@Override
		@RosettaAttribute("tagLine")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("tagLine")
		public String getTagLine() {
			return tagLine;
		}
		
		@RosettaAttribute("tagLine")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("tagLine")
		@Override
		public C2Tag.C2TagBuilder setTagLine(String _tagLine) {
			this.tagLine = _tagLine == null ? null : _tagLine;
			return this;
		}
		
		@Override
		public C2Tag build() {
			return new C2Tag.C2TagImpl(this);
		}
		
		@Override
		public C2Tag.C2TagBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C2Tag.C2TagBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTagLine()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C2Tag.C2TagBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C2Tag.C2TagBuilder o = (C2Tag.C2TagBuilder) other;
			
			
			merger.mergeBasic(getTagLine(), o.getTagLine(), this::setTagLine);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C2Tag _that = getType().cast(o);
		
			if (!Objects.equals(tagLine, _that.getTagLine())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (tagLine != null ? tagLine.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C2TagBuilder {" +
				"tagLine=" + this.tagLine +
			'}';
		}
	}
}
