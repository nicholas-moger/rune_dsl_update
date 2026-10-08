package chaos.s06.a3hub.p1;

import chaos.s06.a3hub.p1.meta.C6TagMeta;
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
 * Self-contained helper - relocated by the import axis.
 * @version 1.0.0
 */
@RosettaDataType(value="C6Tag", builder=C6Tag.C6TagBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C6Tag", model="chaos", builder=C6Tag.C6TagBuilderImpl.class, version="1.0.0")
public interface C6Tag extends RosettaModelObject {

	C6TagMeta metaData = new C6TagMeta();

	/*********************** Getter Methods  ***********************/
	String getCaption();

	/*********************** Build Methods  ***********************/
	C6Tag build();
	
	C6Tag.C6TagBuilder toBuilder();
	
	static C6Tag.C6TagBuilder builder() {
		return new C6Tag.C6TagBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C6Tag> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C6Tag> getType() {
		return C6Tag.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("caption"), String.class, getCaption(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface C6TagBuilder extends C6Tag, RosettaModelObjectBuilder {
		C6Tag.C6TagBuilder setCaption(String caption);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("caption"), String.class, getCaption(), this);
		}
		

		C6Tag.C6TagBuilder prune();
	}

	/*********************** Immutable Implementation of C6Tag  ***********************/
	class C6TagImpl implements C6Tag {
		private final String caption;
		
		protected C6TagImpl(C6Tag.C6TagBuilder builder) {
			this.caption = builder.getCaption();
		}
		
		@Override
		@RosettaAttribute("caption")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("caption")
		public String getCaption() {
			return caption;
		}
		
		@Override
		public C6Tag build() {
			return this;
		}
		
		@Override
		public C6Tag.C6TagBuilder toBuilder() {
			C6Tag.C6TagBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C6Tag.C6TagBuilder builder) {
			ofNullable(getCaption()).ifPresent(builder::setCaption);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C6Tag _that = getType().cast(o);
		
			if (!Objects.equals(caption, _that.getCaption())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (caption != null ? caption.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C6Tag {" +
				"caption=" + this.caption +
			'}';
		}
	}

	/*********************** Builder Implementation of C6Tag  ***********************/
	class C6TagBuilderImpl implements C6Tag.C6TagBuilder {
	
		protected String caption;
		
		@Override
		@RosettaAttribute("caption")
		@Accessor(AccessorType.GETTER)
		@Required
		@RuneAttribute("caption")
		public String getCaption() {
			return caption;
		}
		
		@RosettaAttribute("caption")
		@Accessor(AccessorType.SETTER)
		@Required
		@RuneAttribute("caption")
		@Override
		public C6Tag.C6TagBuilder setCaption(String _caption) {
			this.caption = _caption == null ? null : _caption;
			return this;
		}
		
		@Override
		public C6Tag build() {
			return new C6Tag.C6TagImpl(this);
		}
		
		@Override
		public C6Tag.C6TagBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C6Tag.C6TagBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getCaption()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C6Tag.C6TagBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C6Tag.C6TagBuilder o = (C6Tag.C6TagBuilder) other;
			
			
			merger.mergeBasic(getCaption(), o.getCaption(), this::setCaption);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C6Tag _that = getType().cast(o);
		
			if (!Objects.equals(caption, _that.getCaption())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (caption != null ? caption.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C6TagBuilder {" +
				"caption=" + this.caption +
			'}';
		}
	}
}
